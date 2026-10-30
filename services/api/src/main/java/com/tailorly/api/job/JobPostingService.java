package com.tailorly.api.job;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tailorly.api.job.analysis.JobRequirementsAnalyzer;
import com.tailorly.api.job.dto.CreateJobPostingRequest;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingDetailResponse;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingSummaryResponse;
import com.tailorly.api.job.dto.JobPostingVersionResponse;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.job.reader.JobPostingExtractionService;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousProperties;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lê, grava e versiona vagas de um owner. A leitura da página e a chamada à IA acontecem fora de
 * transação para não segurar conexão nem trava durante operações de rede; só a gravação da
 * versão (com o owner bloqueado, para serializar versões e o limite anônimo) e a gravação da
 * análise são transacionais.
 *
 * <p>Uma versão guarda o conteúdo lido exatamente como foi, para que uma geração futura registre qual
 * versão da vaga usou. Reler a mesma vaga com o mesmo conteúdo reaproveita a versão mais recente; só
 * conteúdo diferente cria uma versão nova. A análise por IA é refeita apenas quando ainda não foi
 * concluída.</p>
 */
@Service
public class JobPostingService {
  private static final Logger log = LoggerFactory.getLogger(JobPostingService.class);
  private static final Set<String> TRACKING_PARAMETERS = Set.of(
      "fbclid", "gclid", "jobboardsource", "source", "ref", "refid", "trk", "trackingid");

  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final JobPostingRepository postings;
  private final JobPostingVersionRepository versions;
  private final JobPostingExtractionService extraction;
  private final JobRequirementsAnalyzer analyzer;
  private final AnonymousQuotaService quota;
  private final AnonymousProperties anonymousProperties;
  private final TransactionTemplate transaction;

  public JobPostingService(OwnerRepository owners, OwnerAccessService access, JobPostingRepository postings,
      JobPostingVersionRepository versions, JobPostingExtractionService extraction,
      JobRequirementsAnalyzer analyzer, AnonymousQuotaService quota, AnonymousProperties anonymousProperties,
      PlatformTransactionManager transactionManager) {
    this.owners = owners;
    this.access = access;
    this.postings = postings;
    this.versions = versions;
    this.extraction = extraction;
    this.analyzer = analyzer;
    this.quota = quota;
    this.anonymousProperties = anonymousProperties;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  /** Resultado de {@link #create}: a versão e se ela foi criada agora ou reaproveitada. */
  public record CreateResult(JobPostingVersionResponse version, boolean created) {
  }

  public CreateResult create(CreateJobPostingRequest request) {
    Owner owner = owners.findById(request.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);

    boolean hasUrl = request.url() != null && !request.url().isBlank();
    boolean hasText = request.text() != null && !request.text().isBlank();
    if (hasUrl == hasText)
      throw new BadRequestException(ErrorCodes.Job.INPUT_REQUIRED);

    JobPostingResponse details = hasUrl
        ? extraction.extract(request.url())
        : extraction.extractFromText(request.text(), request.title(), request.company());

    Saved saved = transaction.execute(status -> save(owner.getId(), details));
    UUID versionId = saved.versionId();

    JobPostingVersion version = versions.findById(versionId).orElseThrow();
    // Refaz a análise se ainda não concluiu ou se foi feita por uma versão anterior do analisador.
    boolean upToDate = JobPostingVersion.COMPLETED.equals(version.getAnalysisStatus())
        && JobRequirementsAnalyzer.VERSION.equals(version.getAnalyzerVersion());
    if (!upToDate)
      runAnalysis(owner, versionId);

    return new CreateResult(toVersionResponse(versions.findById(versionId).orElseThrow(), !saved.created()),
        saved.created());
  }

  public List<JobPostingSummaryResponse> findByOwner(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    return postings.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(this::toSummary).toList();
  }

  public JobPostingDetailResponse findById(UUID id) {
    JobPosting posting = loadAccessible(id).posting();
    List<JobPostingVersion> all = versions.findAllByJobPostingIdOrderByVersionNumberDesc(id);
    JobPostingVersion latest = all.stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Job.NOT_FOUND));
    return new JobPostingDetailResponse(
        toSummary(posting),
        toVersionResponse(latest, false),
        all.stream().map(v -> new JobPostingDetailResponse.VersionInfo(
            v.getVersionNumber(), v.getContentHash(), v.getAnalysisStatus(), v.getCreatedAt())).toList());
  }

  public JobPostingVersionResponse findVersion(UUID id, int versionNumber) {
    loadAccessible(id);
    return toVersionResponse(loadVersion(id, versionNumber), false);
  }

  /** Refaz a análise por IA de uma versão, por exemplo depois de uma falha ou de configurar um provedor. */
  public JobPostingVersionResponse reanalyze(UUID id, int versionNumber) {
    Accessible accessible = loadAccessible(id);
    JobPostingVersion version = loadVersion(id, versionNumber);
    runAnalysis(accessible.owner(), version.getId());
    return toVersionResponse(versions.findById(version.getId()).orElseThrow(), false);
  }

  /** Exclusão: remove a vaga e todas as suas versões, com o texto lido e a análise. */
  public void delete(UUID id) {
    loadAccessible(id);
    transaction.executeWithoutResult(status -> {
      versions.deleteAllByJobPostingId(id);
      postings.deleteById(id);
    });
  }

  private record Saved(UUID versionId, boolean created) {
  }

  private Saved save(UUID ownerId, JobPostingResponse details) {
    // Bloqueia o owner: serializa leituras simultâneas da mesma vaga e a contagem do limite anônimo.
    Owner owner = owners.findByIdForUpdate(ownerId).orElseThrow();
    Instant now = Instant.now();
    String key = sourceKey(details);
    Optional<JobPosting> existing = postings.findByOwnerIdAndSourceKey(ownerId, key);

    if (existing.isPresent()) {
      JobPosting posting = existing.get();
      JobPostingVersion latest = versions
          .findByJobPostingIdAndVersionNumber(posting.getId(), posting.getLatestVersionNumber()).orElseThrow();
      if (latest.getContentHash().equals(details.contentHash())) {
        latest.setLastCheckedAt(now);
        return new Saved(versions.save(latest).getId(), false);
      }
      posting.setLatestVersionNumber(posting.getLatestVersionNumber() + 1);
      refreshListing(posting, details, now);
      postings.save(posting);
      return new Saved(newVersion(posting, details, now).getId(), true);
    }

    if (AnonymousQuotaService.isAnonymous(owner)
        && postings.countByOwnerId(ownerId) >= anonymousProperties.maxJobPostings()) {
      throw new TooManyRequestsException(ErrorCodes.Anonymous.JOB_LIMIT_REACHED);
    }

    JobPosting posting = JobPosting.builder()
        .owner(owner)
        .sourceKind("text".equals(details.platform()) ? "text" : "url")
        .sourceKey(key)
        .latestVersionNumber(1)
        .createdAt(now)
        .build();
    refreshListing(posting, details, now);
    try {
      postings.saveAndFlush(posting);
    } catch (DataIntegrityViolationException duplicate) {
      throw new IllegalStateException("Concurrent creation of the same job posting", duplicate);
    }
    return new Saved(newVersion(posting, details, now).getId(), true);
  }

  private JobPostingVersion newVersion(JobPosting posting, JobPostingResponse details, Instant now) {
    return versions.save(JobPostingVersion.builder()
        .jobPosting(posting)
        .versionNumber(posting.getLatestVersionNumber())
        .contentHash(details.contentHash())
        .details(details)
        .analysisStatus(JobPostingVersion.UNAVAILABLE)
        .createdAt(now)
        .lastCheckedAt(now)
        .build());
  }

  private static void refreshListing(JobPosting posting, JobPostingResponse details, Instant now) {
    posting.setTitle(truncate(details.title(), 512));
    posting.setCompanyName(details.company() == null ? null : truncate(details.company().name(), 512));
    posting.setExpiresAt(truncate(details.expiresAt(), 64));
    posting.setUpdatedAt(now);
  }

  /** Consome a cota anônima só quando a IA realmente será chamada e grava o resultado, qualquer que seja. */
  private void runAnalysis(Owner owner, UUID versionId) {
    JobPostingVersion version = versions.findById(versionId).orElseThrow();
    String status;
    JobAnalysis analysis = null;

    if (!analyzer.isAvailable(owner.getId())) {
      status = JobPostingVersion.UNAVAILABLE;
    } else {
      try {
        quota.consumeExtraction(owner);
        Optional<JobAnalysis> result = analyzer.analyze(owner.getId(), version.getDetails());
        analysis = result.orElse(null);
        status = result.isPresent() ? JobPostingVersion.COMPLETED : JobPostingVersion.UNAVAILABLE;
      } catch (TooManyRequestsException exception) {
        status = JobPostingVersion.QUOTA_EXCEEDED;
      } catch (RuntimeException exception) {
        log.warn("Job analysis failed for version {}", versionId, exception);
        status = JobPostingVersion.FAILED;
      }
    }

    String finalStatus = status;
    JobAnalysis finalAnalysis = analysis;
    transaction.executeWithoutResult(tx -> {
      JobPostingVersion current = versions.findById(versionId).orElseThrow();
      current.setAnalysis(finalAnalysis);
      current.setAnalysisStatus(finalStatus);
      current.setAnalyzerVersion(finalAnalysis == null ? null : finalAnalysis.analyzerVersion());
      current.setAnalyzedAt(JobPostingVersion.COMPLETED.equals(finalStatus) ? Instant.now() : null);
      versions.save(current);
    });
  }

  private record Accessible(JobPosting posting, Owner owner) {
  }

  /** O owner é recarregado porque o da vaga é um proxy preguiçoso, inutilizável fora de uma sessão. */
  private Accessible loadAccessible(UUID id) {
    JobPosting posting = postings.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Job.NOT_FOUND));
    Owner owner = owners.findById(posting.getOwner().getId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    return new Accessible(posting, owner);
  }

  private JobPostingVersion loadVersion(UUID postingId, int versionNumber) {
    return versions.findByJobPostingIdAndVersionNumber(postingId, versionNumber)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Job.NOT_FOUND));
  }

  private JobPostingSummaryResponse toSummary(JobPosting posting) {
    Instant expiresAt = JobPostingExtractionService.parseInstant(posting.getExpiresAt());
    return new JobPostingSummaryResponse(
        posting.getId(), posting.getOwner().getId(), posting.getSourceKind(), posting.getTitle(),
        posting.getCompanyName(), posting.getLatestVersionNumber(),
        expiresAt == null ? null : expiresAt.isBefore(Instant.now()),
        posting.getCreatedAt(), posting.getUpdatedAt());
  }

  private JobPostingVersionResponse toVersionResponse(JobPostingVersion version, boolean reused) {
    return new JobPostingVersionResponse(
        version.getId(), version.getJobPosting().getId(), version.getVersionNumber(), version.getContentHash(),
        JobPostingExtractionService.refreshExpiry(version.getDetails()), version.getAnalysis(),
        version.getAnalysisStatus(), version.getAnalyzedAt(), version.getCreatedAt(), version.getLastCheckedAt(),
        reused);
  }

  /**
   * Identidade da vaga dentro do owner: o ID na plataforma quando há (Gupy), senão o link canônico
   * sem rastreadores, senão o hash do texto. Assim a mesma vaga aberta por links diferentes é uma só.
   */
  static String sourceKey(JobPostingResponse details) {
    String key;
    if ("text".equals(details.platform())) {
      key = "text:" + details.contentHash();
    } else if ("gupy".equals(details.platform()) && details.externalId() != null) {
      key = "gupy:" + details.externalId();
    } else {
      String url = details.canonicalUrl() != null ? details.canonicalUrl() : details.finalUrl();
      key = "url:" + normalizeUrl(url);
    }
    return key.length() <= 1000 ? key : key.substring(0, 900) + "#" + Integer.toHexString(key.hashCode());
  }

  static String normalizeUrl(String url) {
    try {
      URI uri = new URI(url.trim());
      String query = uri.getRawQuery() == null ? "" : java.util.Arrays.stream(uri.getRawQuery().split("&"))
          .filter(parameter -> !parameter.isEmpty())
          .filter(parameter -> {
            String name = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
            return !name.startsWith("utm_") && !TRACKING_PARAMETERS.contains(name);
          })
          .sorted()
          .collect(Collectors.joining("&"));
      String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
      return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
          + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + path + (query.isEmpty() ? "" : "?" + query);
    } catch (URISyntaxException | NullPointerException exception) {
      return url.trim();
    }
  }

  private static String truncate(String value, int max) {
    return value == null ? null : value.length() <= max ? value : value.substring(0, max);
  }
}
