package com.tailorly.api.cv;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.base.snapshot.GtSnapshotItem;
import com.tailorly.api.base.snapshot.GtSnapshotItemRepository;
import com.tailorly.api.base.snapshot.GtSnapshotRepository;
import com.tailorly.api.cv.dto.CvGenerationResponse;
import com.tailorly.api.cv.dto.GenerateCvRequest;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvDocumentBuilder;
import com.tailorly.api.cv.model.CvTailoring;
import com.tailorly.api.cv.model.FactValue;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.cv.pipeline.CvAtsResult;
import com.tailorly.api.cv.pipeline.CvPipeline;
import com.tailorly.api.cv.dto.PreviewCvRequest;
import com.tailorly.api.cv.render.MarkdownCvRenderer;
import com.tailorly.api.cv.render.PdfRenderer;
import com.tailorly.api.cv.template.CvTemplate;
import com.tailorly.api.cv.template.CvTemplateService;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Gera o currículo a partir do snapshot aprovado do Ground Truth e de uma versão da vaga.
 *
 * <p>Os fatos vêm exclusivamente dos itens congelados do snapshot (nenhum OCR, extração ou
 * consulta a fontes externas) e a vaga só ordena o que a pessoa já tem aprovado. O Markdown e o
 * PDF saem do mesmo conteúdo montado; os arquivos vão para o storage privado do owner e a geração
 * registra os três ingredientes usados: snapshot, versão da vaga e versão do template.</p>
 *
 * <p>Três fases: (1) numa transação com o owner bloqueado, resolve as entradas, reaproveita uma
 * geração idêntica sem gastar cota ou consome a cota anônima; (2) fora de transação, roda a esteira
 * de ATS ({@link CvPipeline}), que pode chamar a IA e compilar o PDF várias vezes; (3) numa nova
 * transação, grava os arquivos e o registro. Se a fase 2 ou 3 falhar, a cota é devolvida.</p>
 */
@Service
public class CvGenerationService {
  private static final Logger log = LoggerFactory.getLogger(CvGenerationService.class);
  private static final String PDF = "application/pdf";
  private static final String MARKDOWN = "text/markdown; charset=utf-8";

  private static final String PIPELINE_INPUT_VERSION = CvPipeline.VERSION + "/" + "plan-1";

  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final GtSnapshotRepository snapshots;
  private final GtSnapshotItemRepository snapshotItems;
  private final JobPostingRepository postings;
  private final JobPostingVersionRepository jobVersions;
  private final CvTemplateService templates;
  private final CvGenerationRepository generations;
  private final StoredObjectRepository objects;
  private final ObjectStorage storage;
  private final PdfRenderer pdfRenderer;
  private final MarkdownCvRenderer markdownRenderer;
  private final CvPipeline pipeline;
  private final AnonymousQuotaService quota;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transaction;

  public CvGenerationService(OwnerRepository owners, OwnerAccessService access, GtSnapshotRepository snapshots,
      GtSnapshotItemRepository snapshotItems, JobPostingRepository postings,
      JobPostingVersionRepository jobVersions, CvTemplateService templates, CvGenerationRepository generations,
      StoredObjectRepository objects, ObjectStorage storage, PdfRenderer pdfRenderer,
      MarkdownCvRenderer markdownRenderer, CvPipeline pipeline, AnonymousQuotaService quota,
      ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
    this.owners = owners;
    this.access = access;
    this.snapshots = snapshots;
    this.snapshotItems = snapshotItems;
    this.postings = postings;
    this.jobVersions = jobVersions;
    this.templates = templates;
    this.generations = generations;
    this.objects = objects;
    this.storage = storage;
    this.pdfRenderer = pdfRenderer;
    this.markdownRenderer = markdownRenderer;
    this.pipeline = pipeline;
    this.quota = quota;
    this.objectMapper = objectMapper;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  /** Resultado de {@link #generate}: a geração e se foi produzida agora ou reaproveitada. */
  public record Result(CvGenerationResponse generation, boolean created) {
  }

  /** Arquivo pronto para download. */
  public record Download(byte[] content, String mimeType, String filename) {
  }

  /** Tudo o que a fase 2 precisa, em dados simples (nada de entidades preguiçosas fora da transação). */
  private record Prepared(Result reused, UUID ownerId, UUID snapshotId, UUID jobVersionId, UUID templateId,
      String templateSource, CvDocument base, JobTerms terms, JobAnalysis analysis, String jobTitle,
      CvPreferences preferences, String inputHash) {
  }

  public Result generate(GenerateCvRequest request) {
    CvPreferences preferences = request.preferences() == null ? CvPreferences.defaults() : request.preferences();
    Prepared prepared = transaction.execute(status -> prepare(request, preferences));
    if (prepared.reused() != null) {
      return prepared.reused();
    }
    try {
      CvPipeline.Outcome outcome = pipeline.run(prepared.ownerId(), prepared.base(), prepared.templateSource(),
          prepared.terms(), prepared.analysis(), prepared.jobTitle(), preferences);
      return transaction.execute(status -> persist(prepared, outcome));
    } catch (RuntimeException failure) {
      // A cota foi consumida na fase 1; quem não recebeu um currículo não a perde.
      transaction.executeWithoutResult(status -> owners.findById(prepared.ownerId())
          .ifPresent(quota::refundGeneration));
      throw failure;
    }
  }

  private Prepared prepare(GenerateCvRequest request, CvPreferences preferences) {
    Owner owner = owners.findByIdForUpdate(request.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);

    CvTemplate template = templates.resolveFor(owner, request.templateId());
    GtSnapshot snapshot = resolveSnapshot(owner, request.snapshotId());
    JobPostingVersion jobVersion = resolveJobVersion(owner, request.jobPostingId(), request.jobVersionNumber());

    List<FactValue> facts = snapshotItems.findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(snapshot.getId())
        .stream().map(CvGenerationService::toFact).toList();
    JobTerms terms = JobTerms.of(jobVersion.getDetails(), jobVersion.getAnalysis());
    CvDocumentBuilder.Result built = CvDocumentBuilder.build(facts, terms);
    String inputHash = inputHash(built.document(), preferences, template, jobVersion);

    var existing = generations.findByOwnerIdAndInputHash(owner.getId(), inputHash);
    if (existing.isPresent()) {
      return new Prepared(reuse(existing.get()), null, null, null, null, null, null, null, null, null, null, null);
    }

    quota.consumeGeneration(owner);
    return new Prepared(null, owner.getId(), snapshot.getId(), jobVersion.getId(), template.getId(),
        template.getContent(), built.document(), terms, jobVersion.getAnalysis(),
        jobVersion.getDetails().title(), preferences, inputHash);
  }

  private Result reuse(CvGeneration generation) {
    if (!storage.exists(generation.getPdfObject().getStorageKey())
        || !storage.exists(generation.getMarkdownObject().getStorageKey())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.CvGeneration.ARTIFACT_MISSING);
    }
    return new Result(CvGenerationResponse.from(generation, true), false);
  }

  private Result persist(Prepared prepared, CvPipeline.Outcome outcome) {
    Owner owner = owners.findByIdForUpdate(prepared.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    var concurrent = generations.findByOwnerIdAndInputHash(owner.getId(), prepared.inputHash());
    if (concurrent.isPresent()) {
      // Outra requisição idêntica terminou primeiro: fica a dela e a cota desta volta.
      quota.refundGeneration(owner);
      return reuse(concurrent.get());
    }

    byte[] markdown = markdownRenderer.render(outcome.document());
    String base = "owners/" + owner.getId() + "/generated/" + UUID.randomUUID();
    StoredObject pdfObject = store(owner, "CV_PDF", base + ".pdf", outcome.pdf(), PDF);
    StoredObject markdownObject = store(owner, "CV_MARKDOWN", base + ".md", markdown, MARKDOWN);

    JobPostingVersion jobVersion = jobVersions.findById(prepared.jobVersionId()).orElseThrow();
    CvGeneration generation = generations.saveAndFlush(CvGeneration.builder()
        .owner(owner)
        .snapshot(snapshots.getReferenceById(prepared.snapshotId()))
        .jobPostingVersion(jobVersion)
        .template(templates.reference(prepared.templateId()))
        .inputHash(prepared.inputHash())
        .rendererVersion(truncate(pdfRenderer.fingerprint(), 128))
        .pipelineVersion(CvPipeline.VERSION)
        .preferences(prepared.preferences())
        .document(outcome.document())
        .ats(new CvAtsResult(outcome.report(), outcome.iterations(), outcome.aiStatus()))
        .tailoring(new CvTailoring(prepared.terms().terms(), outcome.report().matchedTerms(),
            jobVersion.getAnalysisStatus()))
        .pdfObject(pdfObject)
        .markdownObject(markdownObject)
        .createdAt(Instant.now())
        .build());
    return new Result(CvGenerationResponse.from(generation, false), true);
  }

  /** Compila o código Typst informado com o conteúdo final de uma geração; não grava nada nem gasta cota. */
  @Transactional(readOnly = true)
  public byte[] preview(UUID generationId, PreviewCvRequest request) {
    CvTemplateService.validateSource(request.content());
    CvGeneration generation = load(generationId);
    return pdfRenderer.render(request.content(), generation.getDocument());
  }

  @Transactional(readOnly = true)
  public List<CvGenerationResponse> findByOwner(UUID ownerId) {
    Owner owner = owners.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    return generations.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
        .map(generation -> CvGenerationResponse.from(generation, false)).toList();
  }

  @Transactional(readOnly = true)
  public CvGenerationResponse findById(UUID id) {
    return CvGenerationResponse.from(load(id), false);
  }

  @Transactional(readOnly = true)
  public Download download(UUID id, String format) {
    CvGeneration generation = load(id);
    boolean pdf = switch (format == null ? "pdf" : format.toLowerCase(Locale.ROOT)) {
      case "pdf" -> true;
      case "markdown", "md" -> false;
      default -> throw new BadRequestException(ErrorCodes.CvGeneration.INVALID_FORMAT);
    };
    StoredObject object = pdf ? generation.getPdfObject() : generation.getMarkdownObject();
    if (!storage.exists(object.getStorageKey())) {
      throw new ResourceNotFoundException(ErrorCodes.CvGeneration.ARTIFACT_MISSING);
    }
    return new Download(storage.get(object.getStorageKey()), object.getMimeType(),
        filename(generation, pdf ? "pdf" : "md"));
  }

  /**
   * Exclusão: remove a geração e os dois arquivos do storage. Os arquivos saem primeiro; se isso
   * falhar, nada foi apagado no banco e a exclusão pode ser repetida.
   */
  @Transactional
  public void delete(UUID id) {
    CvGeneration generation = load(id);
    Owner owner = owners.findByIdForUpdate(generation.getOwner().getId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    StoredObject pdf = generation.getPdfObject();
    StoredObject markdown = generation.getMarkdownObject();
    storage.delete(pdf.getStorageKey());
    storage.delete(markdown.getStorageKey());
    generations.delete(generation);
    generations.flush();
    objects.deleteAll(List.of(pdf, markdown));
  }

  private CvGeneration load(UUID id) {
    CvGeneration generation = generations.findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.NOT_FOUND));
    access.assertCanAccess(generation.getOwner());
    return generation;
  }

  private GtSnapshot resolveSnapshot(Owner owner, UUID requestedId) {
    UUID snapshotId = requestedId != null ? requestedId : owner.getActiveSnapshotId();
    if (snapshotId == null) {
      throw new BadRequestException(ErrorCodes.CvGeneration.NO_ACTIVE_SNAPSHOT);
    }
    GtSnapshot snapshot = snapshots.findById(snapshotId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND));
    // Um snapshot de outro owner é indistinguível de um inexistente.
    if (!snapshot.getOwner().getId().equals(owner.getId())) {
      throw new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND);
    }
    return snapshot;
  }

  private JobPostingVersion resolveJobVersion(Owner owner, UUID postingId, Integer requestedVersion) {
    JobPosting posting = postings.findById(postingId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND));
    if (!posting.getOwner().getId().equals(owner.getId())) {
      throw new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND);
    }
    int versionNumber = requestedVersion != null ? requestedVersion : posting.getLatestVersionNumber();
    return jobVersions.findByJobPostingIdAndVersionNumber(postingId, versionNumber)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND));
  }

  private static FactValue toFact(GtSnapshotItem item) {
    Object value = item.getApprovedValue();
    String text = value instanceof String || value instanceof Number || value instanceof Boolean
        ? value.toString() : null;
    return new FactValue(item.getCategory(), item.getFactRecord().getDisplayOrder(), item.getFieldKey(), text);
  }

  private StoredObject store(Owner owner, String purpose, String key, byte[] content, String mimeType) {
    storage.put(key, content, mimeType);
    // Se a transação for desfeita depois da gravação, o objeto não teria registro algum.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          try {
            storage.delete(key);
          } catch (RuntimeException exception) {
            log.error("Could not remove rolled-back generated file at {}", key, exception);
          }
        }
      }
    });
    return objects.save(StoredObject.builder()
        .owner(owner)
        .purpose(purpose)
        .storageKey(key)
        .contentHash(sha256(content))
        .mimeType(mimeType)
        .sizeBytes((long) content.length)
        // Dados de visitantes herdam o TTL da sessão; contas não expiram.
        .expiresAt(AnonymousQuotaService.isAnonymous(owner) ? owner.getExpiresAt() : null)
        .deletionStatus("ACTIVE")
        .build());
  }

  private String inputHash(CvDocument document, CvPreferences preferences, CvTemplate template,
      JobPostingVersion jobVersion) {
    try {
      // A análise da vaga entra no hash porque ela orienta a IA e a avaliação de ATS.
      byte[] content = objectMapper.writeValueAsBytes(java.util.Arrays.asList(document, preferences,
          jobVersion.getAnalysis(), PIPELINE_INPUT_VERSION));
      return sha256(concat(content, template.getContentHash().getBytes(StandardCharsets.US_ASCII)));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not hash the CV content", exception);
    }
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] joined = new byte[first.length + second.length];
    System.arraycopy(first, 0, joined, 0, first.length);
    System.arraycopy(second, 0, joined, first.length, second.length);
    return joined;
  }

  private static String filename(CvGeneration generation, String extension) {
    var job = generation.getJobPostingVersion().getDetails();
    String label = job.company() != null && job.company().name() != null ? job.company().name() : job.title();
    String slug = label == null ? "" : Normalizer.normalize(label, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    if (slug.length() > 40) {
      slug = slug.substring(0, 40).replaceAll("-+$", "");
    }
    return "curriculo" + (slug.isEmpty() ? "" : "-" + slug) + "." + extension;
  }

  private static String truncate(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }

  private static String sha256(byte[] bytes) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
