package com.tailorly.api.cv;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshot;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
  private static final String PDF = "application/pdf";
  private static final String MARKDOWN = "text/markdown; charset=utf-8";

  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final GtSnapshotRepository snapshots;
  private final JobPostingVersionRepository jobVersions;
  private final CvInputResolver inputs;
  private final CvInputHasher inputHasher;
  private final GeneratedCvFileStore fileStore;
  private final CvTemplateService templates;
  private final CvGenerationRepository generations;
  private final StoredObjectRepository objects;
  private final ObjectStorage storage;
  private final PdfRenderer pdfRenderer;
  private final MarkdownCvRenderer markdownRenderer;
  private final CvPipeline pipeline;
  private final AnonymousQuotaService quota;
  private final TransactionTemplate transaction;

  public CvGenerationService(OwnerRepository owners, OwnerAccessService access, GtSnapshotRepository snapshots,
      JobPostingVersionRepository jobVersions, CvInputResolver inputs, CvInputHasher inputHasher,
      GeneratedCvFileStore fileStore, CvTemplateService templates, CvGenerationRepository generations,
      StoredObjectRepository objects, ObjectStorage storage, PdfRenderer pdfRenderer,
      MarkdownCvRenderer markdownRenderer, CvPipeline pipeline, AnonymousQuotaService quota,
      PlatformTransactionManager transactionManager) {
    this.owners = owners;
    this.access = access;
    this.snapshots = snapshots;
    this.jobVersions = jobVersions;
    this.inputs = inputs;
    this.inputHasher = inputHasher;
    this.fileStore = fileStore;
    this.templates = templates;
    this.generations = generations;
    this.objects = objects;
    this.storage = storage;
    this.pdfRenderer = pdfRenderer;
    this.markdownRenderer = markdownRenderer;
    this.pipeline = pipeline;
    this.quota = quota;
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
    GtSnapshot snapshot = inputs.resolveSnapshot(owner, request.snapshotId());
    JobPostingVersion jobVersion = inputs.resolveJobVersion(owner, request.jobPostingId(),
        request.jobVersionNumber());

    List<FactValue> facts = inputs.factsOf(snapshot);
    JobTerms terms = JobTerms.of(jobVersion.getDetails(), jobVersion.getAnalysis());
    CvDocumentBuilder.Result built = CvDocumentBuilder.build(facts, terms);
    String inputHash = inputHasher.hash(built.document(), preferences, template, jobVersion);

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
    StoredObject pdfObject = fileStore.store(owner, "CV_PDF", base + ".pdf", outcome.pdf(), PDF);
    StoredObject markdownObject = fileStore.store(owner, "CV_MARKDOWN", base + ".md", markdown, MARKDOWN);

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
        CvFilenames.of(generation, pdf ? "pdf" : "md"));
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


  private static String truncate(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }
}
