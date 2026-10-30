package com.tailorly.api.extraction;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.tailorly.api.base.document.BaseDocument;
import com.tailorly.api.base.document.BaseDocumentRepository;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.objects.*;
import com.tailorly.api.owner.*;
import com.tailorly.api.session.AnonymousSession;
import com.tailorly.api.session.AnonymousSessionRepository;
import com.tailorly.api.session.AnonymousSessionTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    // Sem isto, herda `ddl-auto: create` do application.yml padrão e recria o schema a partir
    // das entidades JPA, descartando os DEFAULTs do Flyway (ex.: anonymous_sessions.extractions_count)
    // para qualquer outro teste de integração que reuse o mesmo TAILORLY_TEST_DATABASE_URL.
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class ExtractionPersistenceTest {
  @Autowired ExtractionQueue queue;
  @Autowired ExtractionRunRepository runs;
  @Autowired ExtractedPageRepository pages;
  @Autowired StoredObjectRepository objects;
  @Autowired BaseDocumentRepository documents;
  @Autowired BaseVersionRepository versions;
  @Autowired OwnerRepository owners;
  @Autowired AnonymousSessionRepository anonymousSessions;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("PostgreSQL serializa reenvios, preserva isolamento e recupera execuções interrompidas")
  void queueLifecycle() throws Exception {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    UUID versionId = seed(tx);
    UUID runId;
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> enqueue(tx, versionId));
      var second = executor.submit(() -> enqueue(tx, versionId));
      runId = first.get(10, TimeUnit.SECONDS);
      assertEquals(runId, second.get(10, TimeUnit.SECONDS));
    }
    var work = queue.claim().orElseThrow();
    assertEquals(runId, work.id());
    assertTrue(queue.claim().isEmpty());
    queue.persistPages(runId,
        List.of(new ExtractionQueue.StoredPage(1, "DIRECT", "text/" + runId, "text-hash", 70)));
    assertEquals("PROCESSING", runs.findById(runId).orElseThrow().getStatus());
    queue.complete(runId);
    assertEquals("COMPLETED", runs.findById(runId).orElseThrow().getStatus());
    assertEquals("EXTRACTED", versions.findById(versionId).orElseThrow().getStatus());
    assertEquals(1, pages.findAllByExtractionRunId(runId).size());
    assertEquals(runId, enqueue(tx, versionId));

    // O mesmo hash em outro owner deve ter uma execução própria.
    UUID otherVersion = seed(tx);
    UUID otherRun = enqueue(tx, otherVersion);
    assertNotEquals(runId, otherRun);
    assertEquals(otherRun, queue.claim().orElseThrow().id());
    tx.executeWithoutResult(status -> runs.findById(otherRun).orElseThrow()
        .setStartedAt(Instant.now().minus(Duration.ofMinutes(11))));
    assertTrue(queue.claim().isEmpty());
    assertEquals("WORKER_TIMEOUT", runs.findById(otherRun).orElseThrow().getErrorCode());

    UUID retry = enqueue(tx, otherVersion);
    assertNotEquals(otherRun, retry);
    assertEquals(retry, queue.claim().orElseThrow().id());
    // Falha no segundo insert deve desfazer também o primeiro e a mudança de status.
    assertThrows(RuntimeException.class, () -> queue.persistPages(retry, List.of(
        new ExtractionQueue.StoredPage(1, "DIRECT", "partial-a/" + retry, "a", 10),
        new ExtractionQueue.StoredPage(1, "OCR", "partial-b/" + retry, "b", 10))));
    assertEquals("PROCESSING", runs.findById(retry).orElseThrow().getStatus());
    assertTrue(pages.findAllByExtractionRunId(retry).isEmpty());
    queue.fail(retry, "TEST_FAILURE");
  }

  private UUID enqueue(TransactionTemplate tx, UUID versionId) {
    return tx.execute(status -> {
      var version = versions.findById(versionId).orElseThrow();
      owners.findByIdForUpdate(version.getOwner().getId()).orElseThrow();
      return queue.enqueue(version).getId();
    });
  }

  private UUID seed(TransactionTemplate tx) {
    return tx.execute(status -> {
      Owner owner = owners.save(Owner.builder().kind(OwnerKindEnum.KIND_ANONYMOUS)
          .expiresAt(Instant.now().plusSeconds(3600)).build());
      // AnonymousQuotaService.consumeExtraction exige uma sessão para o owner: sem ela, o
      // UPDATE condicional não encontra linha nenhuma e o primeiro enfileiramento já falha
      // como se a cota estivesse esgotada.
      anonymousSessions.save(AnonymousSession.builder().owner(owner)
          .tokenHash(AnonymousSessionTokens.hash(AnonymousSessionTokens.generate()))
          .expiresAt(owner.getExpiresAt()).build());
      StoredObject original = objects.save(StoredObject.builder().owner(owner).purpose("BASE_ORIGINAL")
          .storageKey("original/" + UUID.randomUUID()).contentHash("same-hash")
          .mimeType("application/pdf").sizeBytes(100L).deletionStatus("ACTIVE").build());
      BaseDocument document = documents.save(BaseDocument.builder().owner(owner).originalObject(original)
          .contentHash("same-hash").originalFilename("fixture.pdf").build());
      return versions.saveAndFlush(BaseVersion.builder().owner(owner).document(document)
          .versionNumber(1).status("CANDIDATE").temporalReviewRequired(true).build()).getId();
    });
  }
}
