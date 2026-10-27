package com.tailorly.api.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.dto.AnonymousMigrationResponse;
import com.tailorly.api.session.dto.MigrateAnonymousSessionRequest;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Exercita no PostgreSQL real a emissão, a limpeza por TTL, as cotas atômicas e a migração do
 * snapshot revisado. Roda só com {@code TAILORLY_TEST_DATABASE_URL} definido (use um schema
 * isolado, por exemplo {@code ?currentSchema=tailorly_test}).
 */
@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.anonymous.cleanup-enabled=false",
    "tailorly.anonymous.max-uploads=3",
    "tailorly.anonymous.max-active-sessions=1000000",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class AnonymousLifecycleIntegrationTest {
  @TestConfiguration
  static class InMemoryStorage {
    @Bean
    @Primary
    FakeStorage fakeStorage() {
      return new FakeStorage();
    }
  }

  static class FakeStorage implements ObjectStorage {
    final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override public byte[] get(String key) { return objects.get(key); }
    @Override public void put(String key, byte[] content, String mimeType) { objects.put(key, content); }
    @Override public boolean exists(String key) { return objects.containsKey(key); }
    @Override public void delete(String key) { objects.remove(key); }
    @Override public void deleteByPrefix(String prefix) { objects.keySet().removeIf(key -> key.startsWith(prefix)); }
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired FakeStorage storage;
  @Autowired AnonymousSessionService sessions;
  @Autowired AnonymousSessionRepository sessionRepository;
  @Autowired AnonymousDataPurgeService purge;
  @Autowired AnonymousMigrationService migration;
  @Autowired AnonymousQuotaService quota;
  @Autowired OwnerRepository owners;

  private int sequence;

  @BeforeEach
  void reset() {
    sequence = 0;
  }

  @Test
  @DisplayName("emite sessão no servidor: só o hash fica no banco, o owner herda o TTL e o token autentica")
  void issuesAndAuthenticates() {
    var issued = sessions.issue();

    assertTrue(issued.token().startsWith(AnonymousSessionTokens.PREFIX));
    assertEquals(0, jdbc.queryForObject(
        "select count(*) from anonymous_sessions where token_hash = ?", Integer.class, issued.token()));
    assertEquals(1, jdbc.queryForObject(
        "select count(*) from anonymous_sessions where token_hash = ?", Integer.class,
        AnonymousSessionTokens.hash(issued.token())));
    Owner owner = owners.findById(issued.ownerId()).orElseThrow();
    assertEquals(OwnerKindEnum.KIND_ANONYMOUS, owner.getKind());
    assertEquals(issued.expiresAt().toEpochMilli(), owner.getExpiresAt().toEpochMilli());

    assertEquals(issued.ownerId(), sessions.authenticate(issued.token()).orElseThrow().ownerId());
    assertTrue(sessions.authenticate(issued.token() + "x").isEmpty());
    assertTrue(sessions.authenticate("eyJ.jwt.like").isEmpty());

    sessions.revoke(issued.sessionId());
    assertTrue(sessions.authenticate(issued.token()).isEmpty());
  }

  @Test
  @DisplayName("sessão expirada não autentica")
  void expiredSessionDoesNotAuthenticate() {
    var issued = sessions.issue();
    jdbc.update("update anonymous_sessions set expires_at = now() - interval '1 minute' where id = ?",
        issued.sessionId());

    assertTrue(sessions.authenticate(issued.token()).isEmpty());
  }

  @Test
  @DisplayName("a limpeza apaga todos os dados do visitante e preserva os de outros owners")
  void purgeRemovesEverythingOfTheAnonymousOwnerOnly() {
    Seed visitor = seedAnonymous(Instant.now().plusSeconds(3600));
    Seed other = seedAnonymous(Instant.now().plusSeconds(3600));

    purge.purgeOwner(visitor.ownerId);

    for (String table : List.of("stored_objects", "base_documents", "base_versions", "gt_snapshots")) {
      assertEquals(0, count("select count(*) from " + table + " where owner_id = ?", visitor.ownerId), table);
    }
    assertEquals(0, count("select count(*) from owners where id = ?", visitor.ownerId));
    assertEquals(0, count("select count(*) from anonymous_sessions where owner_id = ?", visitor.ownerId));
    assertEquals(0, count("select count(*) from facts where id = ?", visitor.factId));
    assertEquals(0, count("select count(*) from fact_evidence where fact_id = ?", visitor.factId));
    assertEquals(0, count("select count(*) from extracted_pages where id = ?", visitor.pageId));
    assertEquals(0, count("select count(*) from review_decisions where actor_owner_id = ?", visitor.ownerId));
    assertTrue(storage.objects.keySet().stream().noneMatch(key -> key.startsWith("owners/" + visitor.ownerId)));

    assertEquals(1, count("select count(*) from owners where id = ?", other.ownerId));
    assertEquals(1, count("select count(*) from facts where id = ?", other.factId));
    assertTrue(storage.objects.keySet().stream().anyMatch(key -> key.startsWith("owners/" + other.ownerId)));
  }

  @Test
  @DisplayName("a limpeza recusa apagar um owner de conta")
  void purgeRefusesAccountOwners() {
    Account account = seedAccount();

    assertThrows(IllegalStateException.class, () -> purge.purgeOwner(account.ownerId));
    assertEquals(1, count("select count(*) from owners where id = ?", account.ownerId));
  }

  @Test
  @DisplayName("o job de limpeza remove só as sessões expiradas")
  void cleanupJobPurgesOnlyExpiredSessions() {
    Seed expired = seedAnonymous(Instant.now().minusSeconds(60));
    Seed active = seedAnonymous(Instant.now().plusSeconds(3600));
    AnonymousCleanupJob job = new AnonymousCleanupJob(sessionRepository, purge);

    int purged = job.purgeExpired(Instant.now());

    assertTrue(purged >= 1);
    assertEquals(0, count("select count(*) from owners where id = ?", expired.ownerId));
    assertEquals(1, count("select count(*) from owners where id = ?", active.ownerId));
  }

  @Test
  @DisplayName("cotas de upload são atômicas: com concorrência, exatamente o limite é concedido")
  void uploadQuotaIsAtomicUnderConcurrency() throws Exception {
    var issued = sessions.issue();
    Owner owner = owners.findById(issued.ownerId()).orElseThrow();
    AtomicInteger granted = new AtomicInteger();
    AtomicInteger denied = new AtomicInteger();

    try (var executor = Executors.newFixedThreadPool(8)) {
      var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
      for (int i = 0; i < 12; i++) {
        futures.add(executor.submit(() -> {
          try {
            quota.consumeUpload(owner);
            granted.incrementAndGet();
          } catch (TooManyRequestsException exception) {
            denied.incrementAndGet();
          }
        }));
      }
      for (var future : futures) {
        future.get();
      }
    }

    assertEquals(3, granted.get());
    assertEquals(9, denied.get());
    assertEquals(3, jdbc.queryForObject(
        "select uploads_count from anonymous_sessions where id = ?", Integer.class, issued.sessionId()));
  }

  @Test
  @DisplayName("migra o snapshot revisado para a conta sem reprocessar e descarta o resto")
  void migratesReviewedSnapshotWithoutReprocessing() {
    Seed visitor = seedAnonymous(Instant.now().plusSeconds(3600));
    Account account = seedAccount();
    int runsBefore = count("select count(*) from extraction_runs where base_version_id = ?", visitor.versionId);

    AnonymousMigrationResponse response = migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest(visitor.token, false));

    assertEquals(visitor.snapshotId, response.snapshotId());
    assertEquals(visitor.versionId, response.baseVersionId());
    assertFalse(response.documentReused());
    assertFalse(response.replacedActive());

    // A cadeia revisada agora pertence à conta.
    assertEquals(account.ownerId, owner("base_versions", visitor.versionId));
    assertEquals(account.ownerId, owner("base_documents", visitor.documentId));
    assertEquals(account.ownerId, owner("gt_snapshots", visitor.snapshotId));
    assertEquals(visitor.snapshotId, jdbc.queryForObject(
        "select active_snapshot_id from owners where id = ?", UUID.class, account.ownerId));
    assertEquals(1, jdbc.queryForObject(
        "select version_number from base_versions where id = ?", Integer.class, visitor.versionId));
    assertEquals(1, jdbc.queryForObject(
        "select snapshot_number from gt_snapshots where id = ?", Integer.class, visitor.snapshotId));
    assertEquals("confirmado", jdbc.queryForObject(
        "select review_state from facts where id = ?", String.class, visitor.factId));
    assertEquals(account.ownerId, jdbc.queryForObject(
        "select actor_owner_id from review_decisions where id = ?", UUID.class, visitor.decisionId));

    // Nenhum OCR/extração novo: a mesma execução concluída segue ligada à versão.
    assertEquals(runsBefore, count("select count(*) from extraction_runs where base_version_id = ?",
        visitor.versionId));
    assertEquals("COMPLETED", jdbc.queryForObject(
        "select status from extraction_runs where id = ?", String.class, visitor.runId));

    // Os objetos foram copiados para o prefixo da conta, sem expiração, e os antigos sumiram.
    List<String> keys = jdbc.queryForList(
        "select storage_key from stored_objects where owner_id = ?", String.class, account.ownerId);
    assertEquals(2, keys.size());
    for (String key : keys) {
      assertTrue(key.startsWith("owners/" + account.ownerId + "/"), key);
      assertTrue(storage.exists(key), key);
    }
    assertEquals(0, count("select count(*) from stored_objects where owner_id = ? and expires_at is not null",
        account.ownerId));
    assertTrue(storage.objects.keySet().stream().noneMatch(key -> key.startsWith("owners/" + visitor.ownerId)));

    // O que não foi migrado (candidata) e a própria sessão deixam de existir.
    assertEquals(0, count("select count(*) from owners where id = ?", visitor.ownerId));
    assertEquals(0, count("select count(*) from base_versions where id = ?", visitor.leftoverVersionId));
    assertEquals(0, count("select count(*) from anonymous_sessions where owner_id = ?", visitor.ownerId));
  }

  @Test
  @DisplayName("sem confirmação, não substitui o Ground Truth ativo da conta e nada muda")
  void refusesToReplaceActiveSnapshotWithoutConfirmation() {
    Seed visitor = seedAnonymous(Instant.now().plusSeconds(3600));
    Account account = seedAccountWithActiveSnapshot();

    assertThrows(ResourceAlreadyExistsException.class, () -> migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest(visitor.token, false)));

    assertEquals(1, count("select count(*) from owners where id = ?", visitor.ownerId));
    assertEquals(visitor.ownerId, owner("base_versions", visitor.versionId));
    assertEquals(account.activeSnapshotId, jdbc.queryForObject(
        "select active_snapshot_id from owners where id = ?", UUID.class, account.ownerId));
  }

  @Test
  @DisplayName("com confirmação, substitui o ativo anterior, que vira SUPERSEDED, e renumera versões")
  void replacesActiveSnapshotWhenConfirmed() {
    Seed visitor = seedAnonymous(Instant.now().plusSeconds(3600));
    Account account = seedAccountWithActiveSnapshot();

    AnonymousMigrationResponse response = migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest(visitor.token, true));

    assertTrue(response.replacedActive());
    assertEquals(visitor.snapshotId, jdbc.queryForObject(
        "select active_snapshot_id from owners where id = ?", UUID.class, account.ownerId));
    assertEquals("SUPERSEDED", jdbc.queryForObject(
        "select status from base_versions where id = ?", String.class, account.activeVersionId));
    assertEquals(2, jdbc.queryForObject(
        "select version_number from base_versions where id = ?", Integer.class, visitor.versionId));
    assertEquals(2, jdbc.queryForObject(
        "select snapshot_number from gt_snapshots where id = ?", Integer.class, visitor.snapshotId));
  }

  @Test
  @DisplayName("se a conta já tem o mesmo arquivo, a versão migrada reaproveita o documento")
  void reusesAccountDocumentWithSameHash() {
    Seed visitor = seedAnonymous(Instant.now().plusSeconds(3600));
    Account account = seedAccount();
    String hash = jdbc.queryForObject("select content_hash from base_documents where id = ?",
        String.class, visitor.documentId);
    UUID existingDocument = insertDocument(account.ownerId, hash, "owners/" + account.ownerId + "/originals/e.pdf");

    AnonymousMigrationResponse response = migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest(visitor.token, false));

    assertTrue(response.documentReused());
    assertEquals(existingDocument, response.documentId());
    assertEquals(existingDocument, jdbc.queryForObject(
        "select document_id from base_versions where id = ?", UUID.class, visitor.versionId));
    assertEquals(0, count("select count(*) from base_documents where id = ?", visitor.documentId));
    assertEquals(1, count("select count(*) from base_documents where owner_id = ? and content_hash = ?",
        account.ownerId, hash));
  }

  @Test
  @DisplayName("sem snapshot revisado não há o que migrar")
  void nothingToMigrateWithoutActiveSnapshot() {
    var issued = sessions.issue();
    Account account = seedAccount();

    assertThrows(BadRequestException.class, () -> migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest(issued.token(), false)));
    assertEquals(1, count("select count(*) from owners where id = ?", issued.ownerId()));
  }

  @Test
  @DisplayName("token inválido ou expirado não migra")
  void invalidTokenIsRejected() {
    Account account = seedAccount();

    assertThrows(BadRequestException.class, () -> migration.migrate(account.authentication,
        new MigrateAnonymousSessionRequest("tlr_anon_nope", false)));
  }

  // ---------------------------------------------------------------- seed helpers

  private record Seed(UUID ownerId, String token, UUID documentId, UUID versionId, UUID runId, UUID pageId,
      UUID factId, UUID decisionId, UUID snapshotId, UUID leftoverVersionId) {
  }

  private record Account(UUID ownerId, Authentication authentication, UUID activeSnapshotId,
      UUID activeVersionId) {
  }

  private Seed seedAnonymous(Instant expiresAt) {
    UUID ownerId = UUID.randomUUID();
    String token = AnonymousSessionTokens.generate();
    jdbc.update("insert into owners (id, kind, revision, expires_at) values (?, 'KIND_ANONYMOUS', 0, ?)",
        ownerId, java.sql.Timestamp.from(expiresAt));
    jdbc.update("insert into anonymous_sessions (id, owner_id, token_hash, expires_at) values (?, ?, ?, ?)",
        UUID.randomUUID(), ownerId, AnonymousSessionTokens.hash(token), java.sql.Timestamp.from(expiresAt));

    String hash = "hash-" + UUID.randomUUID();
    UUID documentId = insertDocument(ownerId, hash, "owners/" + ownerId + "/originals/cv.pdf");
    UUID versionId = insertVersion(ownerId, documentId, 1, "ACTIVE");
    UUID runId = UUID.randomUUID();
    jdbc.update("insert into extraction_runs (id, base_version_id, pipeline_fingerprint, pipeline_versions, "
        + "status, created_at) values (?, ?, 'fp', '{\"implementation\":\"selective-pdf-v2\"}'::jsonb, "
        + "'COMPLETED', now())", runId, versionId);
    UUID pageObject = insertObject(ownerId, "owners/" + ownerId + "/extractions/" + runId + "/pages/1.txt",
        "text/plain");
    UUID pageId = UUID.randomUUID();
    jdbc.update("insert into extracted_pages (id, extraction_run_id, page_number, extraction_method, "
        + "text_object_id, text_hash) values (?, ?, 1, 'DIRECT', ?, 'th')", pageId, runId, pageObject);

    UUID recordId = UUID.randomUUID();
    jdbc.update("insert into fact_records (id, base_version_id, category, display_order) values (?, ?, 'identity', 0)",
        recordId, versionId);
    UUID factId = UUID.randomUUID();
    jdbc.update("insert into facts (id, record_id, extraction_run_id, field_key, extracted_value, reviewed_value, "
        + "origin, review_state) values (?, ?, ?, 'email', '\"a@b.c\"'::jsonb, '\"a@b.c\"'::jsonb, 'ai', 'confirmado')",
        factId, recordId, runId);
    jdbc.update("insert into fact_evidence (id, fact_id, extracted_page_id, source_excerpt, source_locator) "
        + "values (?, ?, ?, 'a@b.c', '{}'::jsonb)", UUID.randomUUID(), factId, pageId);
    UUID issueId = UUID.randomUUID();
    jdbc.update("insert into review_issues (id, base_version_id, fact_id, category, reason, blocking, status) "
        + "values (?, ?, ?, 'identity', 'r', false, 'ANSWERED')", issueId, versionId, factId);
    UUID decisionId = UUID.randomUUID();
    jdbc.update("insert into review_decisions (id, base_version_id, fact_id, issue_id, actor_owner_id, result, "
        + "approved_value, review_revision, decided_at) values (?, ?, ?, ?, ?, 'confirmado', '\"a@b.c\"'::jsonb, 0, now())",
        decisionId, versionId, factId, issueId, ownerId);
    UUID snapshotId = insertSnapshot(ownerId, versionId, 1);
    jdbc.update("insert into gt_snapshot_items (id, snapshot_id, source_fact_id, source_decision_id, record_id, "
        + "category, field_key, approved_value) values (?, ?, ?, ?, ?, 'identity', 'email', '\"a@b.c\"'::jsonb)",
        UUID.randomUUID(), snapshotId, factId, decisionId, recordId);
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", snapshotId, ownerId);

    // Uma segunda versão candidata que não faz parte do snapshot ativo.
    UUID leftoverDocument = insertDocument(ownerId, "hash-" + UUID.randomUUID(),
        "owners/" + ownerId + "/originals/other.pdf");
    UUID leftoverVersion = insertVersion(ownerId, leftoverDocument, 2, "CANDIDATE");

    return new Seed(ownerId, token, documentId, versionId, runId, pageId, factId, decisionId, snapshotId,
        leftoverVersion);
  }

  private Account seedAccount() {
    UUID userId = UUID.randomUUID();
    String email = "account-" + UUID.randomUUID() + "@example.com";
    jdbc.update("insert into users (id, email, password_hash, enabled, created_at) values (?, ?, 'x', true, now())",
        userId, email);
    UUID ownerId = UUID.randomUUID();
    jdbc.update("insert into owners (id, kind, user_id, revision) values (?, 'KIND_ACCOUNT', ?, 0)", ownerId, userId);
    return new Account(ownerId, new UsernamePasswordAuthenticationToken(email, "n/a", List.of()), null, null);
  }

  private Account seedAccountWithActiveSnapshot() {
    Account account = seedAccount();
    UUID documentId = insertDocument(account.ownerId, "hash-" + UUID.randomUUID(),
        "owners/" + account.ownerId + "/originals/old.pdf");
    UUID versionId = insertVersion(account.ownerId, documentId, 1, "ACTIVE");
    UUID snapshotId = insertSnapshot(account.ownerId, versionId, 1);
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", snapshotId, account.ownerId);
    return new Account(account.ownerId, account.authentication, snapshotId, versionId);
  }

  private UUID insertObject(UUID ownerId, String key, String mime) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into stored_objects (id, owner_id, purpose, storage_key, content_hash, mime_type, "
        + "size_bytes, expires_at, deletion_status) values (?, ?, 'TEST', ?, 'h', ?, 3, now() + interval '1 hour', 'ACTIVE')",
        id, ownerId, key, mime);
    storage.objects.put(key, ("bytes-of-" + key).getBytes());
    return id;
  }

  private UUID insertDocument(UUID ownerId, String hash, String key) {
    UUID objectId = insertObject(ownerId, key, "application/pdf");
    UUID id = UUID.randomUUID();
    jdbc.update("insert into base_documents (id, owner_id, original_object_id, content_hash, original_filename, "
        + "page_count, created_at) values (?, ?, ?, ?, 'cv.pdf', 1, now())", id, ownerId, objectId, hash);
    return id;
  }

  private UUID insertVersion(UUID ownerId, UUID documentId, int number, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into base_versions (id, owner_id, document_id, version_number, status, review_revision, "
        + "created_at, temporal_review_required) values (?, ?, ?, ?, ?, 0, now(), false)",
        id, ownerId, documentId, number, status);
    return id;
  }

  private UUID insertSnapshot(UUID ownerId, UUID versionId, int number) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into gt_snapshots (id, owner_id, base_version_id, snapshot_number, approved_review_revision, "
        + "content_hash, approved_at, schema_version) values (?, ?, ?, ?, 0, 'ch', now(), 1)",
        id, ownerId, versionId, number);
    return id;
  }

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }

  private UUID owner(String table, UUID id) {
    return jdbc.queryForObject("select owner_id from " + table + " where id = ?", UUID.class, id);
  }
}
