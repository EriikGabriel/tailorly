package com.tailorly.api.cv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.tailorly.api.cv.dto.CvGenerationResponse;
import com.tailorly.api.cv.dto.GenerateCvRequest;
import com.tailorly.api.cv.dto.PreviewCvRequest;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.pipeline.CvPlanAdvisor;
import com.tailorly.api.cv.render.PdfRenderer;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.session.AnonymousDataPurgeService;
import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.session.AnonymousSessionService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.ServiceUnavailableException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercita no PostgreSQL real a migração V9, a geração a partir do snapshot aprovado, a reutilização
 * de entradas idênticas, a cota anônima, o isolamento entre owners, a exclusão e a purga. Roda só
 * com {@code TAILORLY_TEST_DATABASE_URL}. O Typst é substituído por um compilador falso.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.anonymous.cleanup-enabled=false",
    "tailorly.anonymous.max-generations=2",
    "tailorly.anonymous.max-active-sessions=1000000",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class CvGenerationIntegrationTest {
  static final Map<String, byte[]> STORED = new ConcurrentHashMap<>();
  static final AtomicInteger RENDERS = new AtomicInteger();
  static volatile boolean rendererDown;

  static volatile boolean aiAvailable;
  static volatile boolean aiFails;
  static final AtomicInteger AI_CALLS = new AtomicInteger();

  @TestConfiguration
  static class Fakes {
    /** Substitui a IA: inverte a ordem das tecnologias e pede layout compacto. */
    @Bean
    @Primary
    CvPlanAdvisor fakeAdvisor() {
      return new CvPlanAdvisor(null, null, null) {
        @Override
        public boolean isAvailable(UUID ownerId) {
          return aiAvailable;
        }

        @Override
        public Suggestion suggest(UUID ownerId, CvDocument document, com.tailorly.api.job.dto.JobAnalysis analysis,
            String jobTitle, CvPreferences prefs, com.tailorly.api.cv.ats.AtsReport report) {
          AI_CALLS.incrementAndGet();
          if (aiFails) {
            throw new IllegalStateException("provider down");
          }
          List<String> ids = document.sections().stream().filter(section -> "technologies".equals(section.key()))
              .flatMap(section -> section.entries().stream()).map(CvDocument.Entry::id)
              .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
          java.util.Collections.reverse(ids);
          return new Suggestion(new com.tailorly.api.cv.model.CvPlan(List.of("technologies", "experience"),
              Map.of("technologies", ids), "compact"), "Tecnologias primeiro.");
        }
      };
    }

    @Bean
    @Primary
    ObjectStorage fakeStorage() {
      return new ObjectStorage() {
        @Override public byte[] get(String key) { return STORED.get(key); }
        @Override public void put(String key, byte[] content, String mimeType) { STORED.put(key, content); }
        @Override public boolean exists(String key) { return STORED.containsKey(key); }
        @Override public void delete(String key) { STORED.remove(key); }
        @Override public void deleteByPrefix(String prefix) { STORED.keySet().removeIf(key -> key.startsWith(prefix)); }
      };
    }

    @Bean
    @Primary
    PdfRenderer fakeRenderer() {
      return new PdfRenderer() {
        @Override public String fingerprint() { return "fake-typst 1.0"; }

        @Override
        public byte[] render(String templateSource, CvDocument document) {
          if (rendererDown) {
            throw new ServiceUnavailableException("down");
          }
          RENDERS.incrementAndGet();
          return TestPdfs.of(document);
        }
      };
    }
  }

  @Autowired CvGenerationService service;
  @Autowired com.tailorly.api.cv.template.CvTemplateService templateService;
  @Autowired CvGenerationRepository generations;
  @Autowired AnonymousSessionService sessions;
  @Autowired AnonymousDataPurgeService purge;
  @Autowired JobPostingRepository postings;
  @Autowired JobPostingVersionRepository jobVersions;
  @Autowired JdbcTemplate jdbc;

  private UUID ownerId;
  private UUID snapshotId;
  private UUID jobId;
  private final java.util.List<UUID> extraOwners = new java.util.ArrayList<>();

  @BeforeEach
  void setup() {
    STORED.clear();
    RENDERS.set(0);
    rendererDown = false;
    aiAvailable = false;
    aiFails = false;
    AI_CALLS.set(0);
    var issued = sessions.issue();
    ownerId = issued.ownerId();
    authenticate(issued);
    snapshotId = seedSnapshot(ownerId);
    jobId = seedJob(ownerId, List.of("java"));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    purge.purgeOwner(ownerId);
    extraOwners.forEach(purge::purgeOwner);
  }

  private void authenticate(com.tailorly.api.session.dto.IssuedAnonymousSessionResponse issued) {
    SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
        new AnonymousPrincipal(issued.sessionId(), issued.ownerId(), issued.expiresAt())));
  }

  private GenerateCvRequest request() {
    return new GenerateCvRequest(ownerId, jobId, null, null, null, null);
  }

  @Test
  @DisplayName("gera PDF e Markdown só com os fatos do snapshot e registra snapshot, vaga e template")
  void generatesFromApprovedSnapshotOnly() {
    var result = service.generate(request());

    assertTrue(result.created());
    CvGenerationResponse generation = result.generation();
    assertEquals(snapshotId, generation.groundTruth().snapshotId());
    assertEquals(1, generation.job().versionNumber());
    assertEquals("classic", generation.template().name());
    assertEquals(1, generation.template().version());
    assertEquals("fake-typst 1.0", generation.rendererVersion());
    assertEquals(List.of("java"), generation.matchedTerms());

    String markdown = new String(service.download(generation.id(), "markdown").content(), StandardCharsets.UTF_8);
    assertTrue(markdown.contains("# Ana Souza"));
    assertTrue(markdown.indexOf("Java") < markdown.indexOf("Python"), "a tecnologia pedida pela vaga vem primeiro");
    // A vaga pede Kubernetes, mas isso não é um fato aprovado: nunca entra no currículo.
    assertFalse(markdown.contains("Kubernetes"));
    assertFalse(markdown.contains("Rust"));
    var pdf = service.download(generation.id(), "pdf");
    assertEquals("application/pdf", pdf.mimeType());
    assertEquals("curriculo-acme.pdf", pdf.filename());
    assertTrue(new String(pdf.content(), StandardCharsets.ISO_8859_1).startsWith("%PDF-"));

    assertEquals(2, jdbc.queryForObject("select count(*) from stored_objects where owner_id = ? "
        + "and purpose in ('CV_PDF', 'CV_MARKDOWN') and expires_at is not null", Integer.class, ownerId));
    assertEquals(1, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));
    // Gerar não reexecuta OCR nem extração: nenhuma execução nova existe para as versões do owner.
    assertEquals(0, jdbc.queryForObject("select count(*) from extraction_runs where base_version_id in "
        + "(select id from base_versions where owner_id = ?)", Integer.class, ownerId));
  }

  @Test
  @DisplayName("entrada idêntica reaproveita a geração: sem nova compilação nem consumo de cota")
  void identicalInputIsReused() {
    var first = service.generate(request());
    var again = service.generate(request());

    assertFalse(again.created());
    assertTrue(again.generation().reused());
    assertEquals(first.generation().id(), again.generation().id());
    assertEquals(1, RENDERS.get());
    assertEquals(1, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));
    assertEquals(1, service.findByOwner(ownerId).size());
  }

  @Test
  @DisplayName("versões da vaga geram currículos distintos e a antiga continua endereçável")
  void jobVersionsAreRecorded() {
    var first = service.generate(request());
    addJobVersion(jobId, List.of("python"));

    var latest = service.generate(request());
    var pinned = service.generate(new GenerateCvRequest(ownerId, jobId, 1, null, null, null));

    assertEquals(2, latest.generation().job().versionNumber());
    assertNotEquals(first.generation().id(), latest.generation().id());
    assertEquals(first.generation().id(), pinned.generation().id());
    assertEquals(first.generation().job().versionId(), pinned.generation().job().versionId());
    assertThrows(ResourceNotFoundException.class,
        () -> service.generate(new GenerateCvRequest(ownerId, jobId, 9, null, null, null)));
  }

  @Test
  @DisplayName("aplica o limite anônimo de gerações no servidor")
  void enforcesAnonymousGenerationLimit() {
    service.generate(request());
    addJobVersion(jobId, List.of("python"));
    service.generate(request());
    addJobVersion(jobId, List.of("go"));

    assertThrows(TooManyRequestsException.class, () -> service.generate(request()));
    assertEquals(2, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));
  }

  @Test
  @DisplayName("se o PDF não compila, nada é gravado e a cota não é consumida")
  void rendererFailureRollsBackEverything() {
    rendererDown = true;

    assertThrows(ServiceUnavailableException.class, () -> service.generate(request()));

    assertEquals(0, jdbc.queryForObject("select count(*) from cv_generations where owner_id = ?",
        Integer.class, ownerId));
    assertEquals(0, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));
    assertEquals(0, jdbc.queryForObject("select count(*) from stored_objects where owner_id = ? "
        + "and purpose like 'CV_%'", Integer.class, ownerId));
  }

  @Test
  @DisplayName("exige snapshot ativo e não usa vaga, snapshot ou geração de outro owner")
  void isolatesOwners() {
    var otherIssued = sessions.issue();
    extraOwners.add(otherIssued.ownerId());
    UUID otherJob = seedJob(otherIssued.ownerId(), List.of("java"));
    UUID otherSnapshot = seedSnapshot(otherIssued.ownerId());
    var generation = service.generate(request()).generation();

    assertThrows(ResourceNotFoundException.class,
        () -> service.generate(new GenerateCvRequest(ownerId, otherJob, null, null, null, null)));
    assertThrows(ResourceNotFoundException.class,
        () -> service.generate(new GenerateCvRequest(ownerId, jobId, null, otherSnapshot, null, null)));
    assertThrows(ResourceNotFoundException.class,
        () -> service.generate(new GenerateCvRequest(ownerId, jobId, null, null, UUID.randomUUID(), null)));
    assertThrows(BadRequestException.class, () -> service.download(generation.id(), "docx"));

    jdbc.update("update owners set active_snapshot_id = null where id = ?", ownerId);
    assertThrows(BadRequestException.class, () -> service.generate(request()));

    authenticate(otherIssued);
    assertThrows(AccessDeniedException.class, () -> service.findById(generation.id()));
    assertThrows(AccessDeniedException.class, () -> service.download(generation.id(), "pdf"));
    assertThrows(AccessDeniedException.class, () -> service.delete(generation.id()));
    assertThrows(AccessDeniedException.class, () -> service.findByOwner(ownerId));
    assertTrue(service.findByOwner(otherIssued.ownerId()).isEmpty());
  }

  @Test
  @DisplayName("excluir remove a geração, os objetos e os arquivos do storage")
  void deleteRemovesArtifacts() {
    var generation = service.generate(request()).generation();
    assertEquals(2, STORED.keySet().stream().filter(key -> key.contains("/generated/")).count());

    service.delete(generation.id());

    assertEquals(0, STORED.keySet().stream().filter(key -> key.contains("/generated/")).count());
    assertEquals(0, jdbc.queryForObject("select count(*) from cv_generations where owner_id = ?",
        Integer.class, ownerId));
    assertEquals(0, jdbc.queryForObject("select count(*) from stored_objects where owner_id = ? "
        + "and purpose like 'CV_%'", Integer.class, ownerId));
    assertThrows(ResourceNotFoundException.class, () -> service.findById(generation.id()));
  }

  @Test
  @DisplayName("a purga do visitante apaga gerações e arquivos gerados")
  void purgeRemovesGeneratedCurricula() {
    service.generate(request());

    purge.purgeOwner(ownerId);

    assertEquals(0, jdbc.queryForObject("select count(*) from cv_generations where owner_id = ?",
        Integer.class, ownerId));
    assertTrue(STORED.keySet().stream().noneMatch(key -> key.startsWith("owners/" + ownerId + "/")));
  }

  @Test
  @DisplayName("sem IA a esteira mede a nota de ATS lendo o PDF e registra a avaliação")
  void recordsAtsReportWithoutAi() {
    var generation = service.generate(request()).generation();

    assertEquals("unavailable", generation.ats().aiStatus());
    assertEquals(1, generation.ats().iterations().size());
    var report = generation.ats().report();
    assertEquals(report.score(), report.checks().stream().mapToInt(c -> c.points()).sum());
    assertTrue(report.checks().stream().anyMatch(c -> c.id().equals("parsing") && c.points() > 0),
        "o texto do PDF foi lido");
    assertEquals(List.of("java"), report.matchedTerms());
    assertEquals(List.of("Kubernetes"), report.missingTerms());
    assertEquals(0, AI_CALLS.get());
  }

  @Test
  @DisplayName("com IA, a esteira itera o layout, escolhe a melhor versão e nunca altera o texto")
  void aiRefinesPresentationOnly() {
    aiAvailable = true;

    var generation = service.generate(request()).generation();

    assertEquals("completed", generation.ats().aiStatus());
    assertTrue(generation.ats().iterations().size() >= 2);
    assertEquals("ai", generation.ats().iterations().get(1).source());
    assertEquals("Tecnologias primeiro.", generation.ats().iterations().get(1).rationale());
    int best = generation.ats().iterations().stream().mapToInt(i -> i.score()).max().orElseThrow();
    assertEquals(best, generation.ats().report().score());

    String markdown = new String(service.download(generation.id(), "markdown").content(), StandardCharsets.UTF_8);
    for (String fact : List.of("Ana Souza", "ana@example.com", "Acme", "Engenheira", "Python", "Java", "Go")) {
      assertTrue(markdown.contains(fact), fact);
    }
    assertFalse(markdown.contains("Kubernetes"));
    // Só os fatos do snapshot (e os títulos de seção) existem no documento final.
    String document = com.tailorly.api.cv.ats.AtsEvaluator.flatten(
        jdbcDocument(generation.id()));
    for (String word : document.split("[\\s,;|()]+")) {
      assertTrue(word.isBlank() || List.of("Ana", "Souza", "ana@example.com", "Engenheira", "Acme", "—", "Tecnologias",
          "Experiência", "profissional", "Python", "Java", "Go").contains(word), "texto inesperado: " + word);
    }
  }

  @Test
  @DisplayName("se a IA falha, as regras assumem e a geração termina com o status registrado")
  void aiFailureFallsBackToRules() {
    aiAvailable = true;
    aiFails = true;

    var generation = service.generate(request()).generation();

    assertEquals("failed", generation.ats().aiStatus());
    assertEquals(1, AI_CALLS.get(), "depois de falhar, a esteira não insiste na IA");
    assertEquals(1, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));
  }

  @Test
  @DisplayName("otimização desligada não chama a IA; outras preferências geram outra entrada")
  void preferencesControlThePipeline() {
    aiAvailable = true;
    var off = new CvPreferences(List.of(), false);

    var plain = service.generate(new GenerateCvRequest(ownerId, jobId, null, null, null, off)).generation();
    var withGuidelines = service.generate(new GenerateCvRequest(ownerId, jobId, null, null, null,
        new CvPreferences(List.of("Destacar Liderança Técnica"), false))).generation();

    assertEquals("skipped", plain.ats().aiStatus());
    assertEquals(0, AI_CALLS.get());
    assertNotEquals(plain.id(), withGuidelines.id());
    assertEquals(List.of("Destacar Liderança Técnica"), withGuidelines.preferences().guidelines());
  }

  @Test
  @DisplayName("template editado vira uma versão do owner, é usado nas gerações e tem prévia ao vivo")
  void customTemplateAndPreview() {
    String source = "#let cv = json(\"data.json\")\n= #cv.name\n";
    var bundled = templateService.bundled();
    assertFalse(bundled.custom());
    assertEquals(bundled.id(), templateService.current(ownerId).id());

    var saved = templateService.save(ownerId, source);
    assertTrue(saved.custom());
    assertEquals(1, saved.version());
    assertEquals(saved.id(), templateService.save(ownerId, source).id(), "o mesmo conteúdo não cria versão");
    assertEquals(2, templateService.save(ownerId, source + "// v2\n").version());

    var generation = service.generate(request()).generation();
    assertTrue(generation.template().custom());
    assertEquals(2, generation.template().version());

    int rendersBefore = RENDERS.get();
    byte[] preview = service.preview(generation.id(), new PreviewCvRequest(source));
    assertTrue(new String(preview, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
    assertEquals(rendersBefore + 1, RENDERS.get(), "a prévia compila uma vez");
    assertEquals(1, jdbc.queryForObject("select generations_count from anonymous_sessions where owner_id = ?",
        Integer.class, ownerId));

    // A geração pode voltar a usar o embarcado, explicitamente.
    var withBundled = service.generate(new GenerateCvRequest(ownerId, jobId, null, null, bundled.id(), null));
    assertFalse(withBundled.generation().template().custom());
  }

  @Test
  @DisplayName("template com pacote remoto é recusado e o de outro owner não é visível")
  void templateGuards() {
    assertThrows(BadRequestException.class,
        () -> templateService.save(ownerId, "#import \"@preview/cetz:0.2.0\": canvas"));
    assertThrows(BadRequestException.class,
        () -> service.preview(service.generate(request()).generation().id(),
            new PreviewCvRequest("#import \"@local/x:1.0.0\": y")));

    var mine = templateService.save(ownerId, "= meu\n");
    var otherIssued = sessions.issue();
    extraOwners.add(otherIssued.ownerId());
    authenticate(otherIssued);
    assertThrows(AccessDeniedException.class, () -> templateService.findById(mine.id()));
    assertThrows(AccessDeniedException.class, () -> templateService.current(ownerId));
    assertThrows(ResourceNotFoundException.class,
        () -> service.generate(new GenerateCvRequest(otherIssued.ownerId(), jobId, null, null, mine.id(), null)));
  }

  @Test
  @DisplayName("a purga também apaga os templates editados")
  void purgeRemovesCustomTemplates() {
    templateService.save(ownerId, "= meu\n");

    purge.purgeOwner(ownerId);

    assertEquals(0, jdbc.queryForObject("select count(*) from cv_templates where owner_id = ?",
        Integer.class, ownerId));
  }

  private CvDocument jdbcDocument(UUID generationId) {
    return generations.findWithDetailsById(generationId).orElseThrow().getDocument();
  }

  // ------------------------------------------------------------------ dados

  private UUID seedJob(UUID owner, List<String> skills) {
    Instant now = Instant.now();
    JobPosting posting = postings.saveAndFlush(JobPosting.builder()
        .owner(jdbcOwner(owner)).sourceKind("text").sourceKey("text:" + UUID.randomUUID())
        .title("Dev Java").companyName("Acme").latestVersionNumber(1).createdAt(now).updatedAt(now).build());
    saveJobVersion(posting, 1, skills);
    return posting.getId();
  }

  private void addJobVersion(UUID postingId, List<String> skills) {
    JobPosting posting = postings.findById(postingId).orElseThrow();
    posting.setLatestVersionNumber(posting.getLatestVersionNumber() + 1);
    postings.saveAndFlush(posting);
    saveJobVersion(posting, posting.getLatestVersionNumber(), skills);
  }

  private void saveJobVersion(JobPosting posting, int number, List<String> skills) {
    Instant now = Instant.now();
    jobVersions.saveAndFlush(JobPostingVersion.builder()
        .jobPosting(posting).versionNumber(number).contentHash("hash-" + UUID.randomUUID())
        .details(JobPostingResponse.builder().title("Dev Java")
            .company(new JobPostingResponse.Company("Acme", null, null, null, null, Map.of()))
            .skills(java.util.stream.Stream.concat(skills.stream(), java.util.stream.Stream.of("Kubernetes")).toList())
            .build())
        .analysisStatus(JobPostingVersion.UNAVAILABLE).createdAt(now).lastCheckedAt(now).build());
  }

  private com.tailorly.api.owner.Owner jdbcOwner(UUID id) {
    return ownerRepository.findById(id).orElseThrow();
  }

  @Autowired com.tailorly.api.owner.OwnerRepository ownerRepository;

  /** Cadeia mínima aprovada: versão ativa, fatos confirmados, decisões e itens congelados do snapshot. */
  private UUID seedSnapshot(UUID owner) {
    UUID objectId = UUID.randomUUID();
    jdbc.update("insert into stored_objects (id, owner_id, purpose, storage_key, content_hash, mime_type, size_bytes, "
        + "expires_at, deletion_status) values (?, ?, 'BASE_ORIGINAL', ?, 'h', 'application/pdf', 3, "
        + "now() + interval '1 hour', 'ACTIVE')", objectId, owner, "owners/" + owner + "/originals/cv.pdf");
    UUID documentId = UUID.randomUUID();
    jdbc.update("insert into base_documents (id, owner_id, original_object_id, content_hash, original_filename, "
        + "page_count, created_at) values (?, ?, ?, ?, 'cv.pdf', 1, now())",
        documentId, owner, objectId, "hash-" + UUID.randomUUID());
    UUID versionId = UUID.randomUUID();
    jdbc.update("insert into base_versions (id, owner_id, document_id, version_number, status, review_revision, "
        + "created_at, temporal_review_required) values (?, ?, ?, 1, 'ACTIVE', 0, now(), false)",
        versionId, owner, documentId);
    UUID snapshot = UUID.randomUUID();
    jdbc.update("insert into gt_snapshots (id, owner_id, base_version_id, snapshot_number, approved_review_revision, "
        + "content_hash, approved_at, schema_version) values (?, ?, ?, 1, 0, 'snapshot-hash', now(), 1)",
        snapshot, owner, versionId);

    UUID identity = record(versionId, "identity", 0);
    UUID experience = record(versionId, "experience", 1);
    UUID technologies = record(versionId, "technologies", 2);
    fact(owner, versionId, snapshot, identity, "identity", "name", "Ana Souza");
    fact(owner, versionId, snapshot, identity, "identity", "email", "ana@example.com");
    fact(owner, versionId, snapshot, experience, "experience", "0:company", "Acme");
    fact(owner, versionId, snapshot, experience, "experience", "0:role", "Engenheira");
    fact(owner, versionId, snapshot, technologies, "technologies", "0:name", "Python");
    fact(owner, versionId, snapshot, technologies, "technologies", "1:name", "Java");
    fact(owner, versionId, snapshot, technologies, "technologies", "2:name", "Go");
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", snapshot, owner);
    return snapshot;
  }

  private UUID record(UUID versionId, String category, int order) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into fact_records (id, base_version_id, category, display_order) values (?, ?, ?, ?)",
        id, versionId, category, order);
    return id;
  }

  private void fact(UUID owner, UUID versionId, UUID snapshot, UUID record, String category, String key, String value) {
    String json = "\"" + value + "\"";
    UUID factId = UUID.randomUUID();
    jdbc.update("insert into facts (id, record_id, field_key, extracted_value, reviewed_value, origin, review_state) "
        + "values (?, ?, ?, ?::jsonb, ?::jsonb, 'ai', 'confirmado')", factId, record, key, json, json);
    UUID decisionId = UUID.randomUUID();
    jdbc.update("insert into review_decisions (id, base_version_id, fact_id, actor_owner_id, result, approved_value, "
        + "review_revision, decided_at) values (?, ?, ?, ?, 'confirmado', ?::jsonb, 0, now())",
        decisionId, versionId, factId, owner, json);
    jdbc.update("insert into gt_snapshot_items (id, snapshot_id, source_fact_id, source_decision_id, record_id, "
        + "category, field_key, approved_value) values (?, ?, ?, ?, ?, ?, ?, ?::jsonb)",
        UUID.randomUUID(), snapshot, factId, decisionId, record, category, key, json);
  }
}
