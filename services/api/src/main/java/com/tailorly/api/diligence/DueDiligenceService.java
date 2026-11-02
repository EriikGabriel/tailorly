package com.tailorly.api.diligence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tailorly.api.diligence.DueDiligenceAnalyzer.Gathered;
import com.tailorly.api.diligence.dto.CreateDueDiligenceRequest;
import com.tailorly.api.diligence.dto.DueDiligenceReport;
import com.tailorly.api.diligence.dto.DueDiligenceReport.Entity;
import com.tailorly.api.diligence.dto.DueDiligenceResponse;
import com.tailorly.api.diligence.dto.DueDiligenceSummaryResponse;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.ServiceUnavailableException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executa e guarda due diligences de empresas. A busca e a IA rodam fora de transação (são operações de rede);
 * só a gravação do relatório é uma escrita simples. Cada análise concluída vira um registro novo do owner,
 * consultável depois e removido com a conta ou a sessão anônima. Nada aqui lê ou altera o Ground Truth.
 */
@Service
public class DueDiligenceService {
  private static final Logger log = LoggerFactory.getLogger(DueDiligenceService.class);
  static final int REUSE_DAYS = 30;
  private static final Duration RUNNING_TIMEOUT = Duration.ofMinutes(10);

  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final DueDiligenceRepository repository;
  private final JobPostingRepository postings;
  private final JobPostingVersionRepository versions;
  private final DueDiligenceAnalyzer analyzer;
  private final AnonymousQuotaService quota;
  private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();

  public DueDiligenceService(OwnerRepository owners, OwnerAccessService access, DueDiligenceRepository repository,
      JobPostingRepository postings, JobPostingVersionRepository versions, DueDiligenceAnalyzer analyzer,
      AnonymousQuotaService quota) {
    this.owners = owners;
    this.access = access;
    this.repository = repository;
    this.postings = postings;
    this.versions = versions;
    this.analyzer = analyzer;
    this.quota = quota;
  }

  /** Pedido manual: executa na hora, grava o relatório e o devolve. */
  public DueDiligenceResponse create(CreateDueDiligenceRequest request) {
    Owner owner = loadOwner(request.ownerId());
    Entity entity = resolveEntity(owner, request);
    assertAvailable(owner.getId());

    Instant consultedAt = Instant.now();
    DueDiligenceReport report;
    try {
      report = run(owner, entity, consultedAt);
    } catch (TooManyRequestsException limit) {
      throw limit;
    } catch (RuntimeException failure) {
      log.warn("Due diligence failed for owner {}", owner.getId(), failure);
      throw new ServiceUnavailableException(ErrorCodes.Diligence.ANALYSIS_FAILED);
    }

    DueDiligence saved = repository.save(DueDiligence.builder()
        .owner(owner)
        .companyName(entity.name())
        .jobPostingId(entity.jobPostingId())
        .jobVersionNumber(entity.jobVersionNumber())
        .analyzerVersion(DueDiligenceAnalyzer.VERSION)
        .status(DueDiligence.COMPLETED)
        .triggerKind(DueDiligence.MANUAL)
        .report(report)
        .createdAt(consultedAt)
        .build());
    return toResponse(saved);
  }

  /**
   * Etapa da esteira, logo depois do Ground Truth: ao ler uma vaga, consulta a empresa em segundo plano
   * para que o resultado esteja pronto antes da geração. Só dispara quando a conta tem um Ground Truth
   * ativo, a vaga nomeia a empresa e busca e IA estão disponíveis; não repete uma consulta da mesma empresa
   * feita nos últimos {@value #REUSE_DAYS} dias; nunca falha nem atrasa a leitura da vaga. Sessões anônimas
   * não disparam sozinhas (não gastam a própria cota sem pedir) e podem pedir a análise manualmente.
   */
  public void startForJob(Owner owner, UUID jobPostingId) {
    try {
      if (AnonymousQuotaService.isAnonymous(owner) || owner.getActiveSnapshotId() == null
          || !analyzer.isSearchConfigured() || !analyzer.isAiAvailable(owner.getId()))
        return;
      Entity entity = resolveEntity(owner, new CreateDueDiligenceRequest(owner.getId(), null, jobPostingId));
      Instant now = Instant.now();
      boolean covered = repository.findRecentByCompany(owner.getId(), entity.name(),
          now.minus(Duration.ofDays(REUSE_DAYS))).stream().anyMatch(entry -> !isStale(entry, now));
      if (covered)
        return;

      DueDiligence row = repository.save(DueDiligence.builder()
          .owner(owner)
          .companyName(entity.name())
          .jobPostingId(entity.jobPostingId())
          .jobVersionNumber(entity.jobVersionNumber())
          .analyzerVersion(DueDiligenceAnalyzer.VERSION)
          .status(DueDiligence.RUNNING)
          .triggerKind(DueDiligence.PIPELINE)
          .createdAt(now)
          .build());
      UUID id = row.getId();
      UUID ownerId = owner.getId();
      background.submit(() -> complete(id, ownerId, entity));
    } catch (RuntimeException ignored) {
      // A due diligence é um apoio: a leitura da vaga segue mesmo que ela não possa começar.
      log.info("Pipeline due diligence not started: {}", ignored.getClass().getSimpleName());
    }
  }

  private void complete(UUID id, UUID ownerId, Entity entity) {
    Instant consultedAt = Instant.now();
    DueDiligenceReport report = null;
    try {
      // Contas não têm cota; o owner só é passado para a cota anônima, que não dispara sozinha.
      report = run(null, ownerId, entity, consultedAt);
    } catch (RuntimeException failure) {
      log.warn("Pipeline due diligence {} failed", id, failure);
    }
    DueDiligence entry = repository.findById(id).orElse(null);
    if (entry == null)
      return; // Excluída enquanto rodava.
    entry.setReport(report);
    entry.setStatus(report == null ? DueDiligence.FAILED : DueDiligence.COMPLETED);
    repository.save(entry);
  }

  private DueDiligenceReport run(Owner owner, Entity entity, Instant consultedAt) {
    return run(owner, owner.getId(), entity, consultedAt);
  }

  /** Busca e IA fora de transação; a cota anônima só é gasta quando a IA realmente será chamada. */
  private DueDiligenceReport run(Owner quotaOwner, UUID ownerId, Entity entity, Instant consultedAt) {
    Gathered gathered = analyzer.gather(entity.name());
    if (quotaOwner != null && gathered.hasSources())
      quota.consumeExtraction(quotaOwner);
    return analyzer.analyze(ownerId, entity, gathered, consultedAt);
  }

  private void assertAvailable(UUID ownerId) {
    if (!analyzer.isSearchConfigured())
      throw new ServiceUnavailableException(ErrorCodes.Diligence.SEARCH_UNAVAILABLE);
    if (!analyzer.isAiAvailable(ownerId))
      throw new ServiceUnavailableException(ErrorCodes.Diligence.AI_UNAVAILABLE);
  }

  /** Um registro {@code running} muito antigo é de uma execução interrompida (por exemplo, reinício da API). */
  private static boolean isStale(DueDiligence entry, Instant now) {
    return DueDiligence.RUNNING.equals(entry.getStatus())
        && entry.getCreatedAt().isBefore(now.minus(RUNNING_TIMEOUT));
  }

  private static String effectiveStatus(DueDiligence entry) {
    return isStale(entry, Instant.now()) ? DueDiligence.FAILED : entry.getStatus();
  }

  @PreDestroy
  void shutdown() {
    background.shutdown();
  }

  public List<DueDiligenceSummaryResponse> findByOwner(UUID ownerId) {
    loadOwner(ownerId);
    return repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toSummary).toList();
  }

  public DueDiligenceResponse findById(UUID id) {
    return toResponse(loadAccessible(id));
  }

  public void delete(UUID id) {
    repository.delete(loadAccessible(id));
  }

  private Entity resolveEntity(Owner owner, CreateDueDiligenceRequest request) {
    String name = request.companyName() == null ? "" : request.companyName().strip();
    if (request.jobPostingId() == null) {
      if (name.length() < 2)
        throw new BadRequestException(ErrorCodes.Diligence.COMPANY_REQUIRED);
      return new Entity(name, null, null, null, null);
    }

    JobPosting posting = postings.findById(request.jobPostingId())
        .filter(candidate -> candidate.getOwner().getId().equals(owner.getId()))
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Job.NOT_FOUND));
    if (name.length() < 2)
      name = posting.getCompanyName() == null ? "" : posting.getCompanyName().strip();
    if (name.length() < 2)
      throw new BadRequestException(ErrorCodes.Diligence.COMPANY_REQUIRED);

    String website = versions.findByJobPostingIdAndVersionNumber(posting.getId(), posting.getLatestVersionNumber())
        .map(JobPostingVersion::getDetails)
        .map(details -> details.company())
        .map(company -> company.website())
        .orElse(null);
    return new Entity(name, website, posting.getId(), posting.getLatestVersionNumber(), posting.getTitle());
  }

  private Owner loadOwner(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    return owner;
  }

  private DueDiligence loadAccessible(UUID id) {
    DueDiligence entry = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Diligence.NOT_FOUND));
    loadOwner(entry.getOwner().getId());
    return entry;
  }

  private DueDiligenceSummaryResponse toSummary(DueDiligence entry) {
    DueDiligenceReport report = entry.getReport();
    int withEvidence = report == null ? 0 : (int) report.factors().stream()
        .filter(factor -> DueDiligenceParser.EVIDENCE_FOUND.equals(factor.status())).count();
    return new DueDiligenceSummaryResponse(entry.getId(), effectiveStatus(entry), entry.getTriggerKind(),
        entry.getCompanyName(), report == null ? null : report.entity().jobTitle(), entry.getJobPostingId(),
        withEvidence, DiligenceFactor.values().length, entry.getCreatedAt());
  }

  private DueDiligenceResponse toResponse(DueDiligence entry) {
    return new DueDiligenceResponse(entry.getId(), entry.getOwner().getId(), entry.getCompanyName(),
        entry.getJobPostingId(), entry.getJobVersionNumber(), entry.getAnalyzerVersion(), effectiveStatus(entry),
        entry.getTriggerKind(), entry.getReport(), entry.getCreatedAt());
  }
}
