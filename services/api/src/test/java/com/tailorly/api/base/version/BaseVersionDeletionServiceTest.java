package com.tailorly.api.base.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.tailorly.api.objects.storage.ObjectStorage;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercita no PostgreSQL real a exclusão de uma {@link BaseVersion}: arquivo, texto extraído,
 * fatos, pendências, snapshot e currículos gerados derivados dela devem sumir junto, sem afetar
 * outras versões ou owners. Roda só com {@code TAILORLY_TEST_DATABASE_URL} definido.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.anonymous.cleanup-enabled=false",
    "tailorly.account.cleanup-enabled=false",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class BaseVersionDeletionServiceTest {
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
  @Autowired BaseVersionDeletionService deletionService;

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("exclui a versão ativa e tudo derivado dela: fatos, pendências, snapshot e a geração")
  void deletesActiveVersionAndDerivedGeneration() {
    Account account = seedAccount();
    Seed seed = seedFullVersion(account.ownerId, 1, "ACTIVE");
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", seed.snapshotId, account.ownerId);
    UUID pdfObject = insertObject(account.ownerId, "owners/" + account.ownerId + "/generated/cv.pdf", "application/pdf");
    UUID mdObject = insertObject(account.ownerId, "owners/" + account.ownerId + "/generated/cv.md", "text/markdown");
    UUID generationId = insertGeneration(account.ownerId, seed.snapshotId, pdfObject, mdObject);
    authenticateAs(account);

    deletionService.delete(seed.versionId);

    for (String table : List.of("base_versions", "base_documents", "extraction_runs", "gt_snapshots")) {
      assertEquals(0, count("select count(*) from " + table + " where id = ?",
          idFor(table, seed)), table);
    }
    assertEquals(0, count("select count(*) from facts where id = ?", seed.factId));
    assertEquals(0, count("select count(*) from fact_evidence where fact_id = ?", seed.factId));
    assertEquals(0, count("select count(*) from extracted_pages where id = ?", seed.pageId));
    assertEquals(0, count("select count(*) from review_issues where id = ?", seed.issueId));
    assertEquals(0, count("select count(*) from review_decisions where id = ?", seed.decisionId));
    assertEquals(0, count("select count(*) from cv_generations where id = ?", generationId));
    assertNull(jdbc.queryForObject("select active_snapshot_id from owners where id = ?",
        UUID.class, account.ownerId));
    assertFalse(storage.exists("owners/" + account.ownerId + "/generated/cv.pdf"));
    assertFalse(storage.exists("owners/" + account.ownerId + "/generated/cv.md"));
    assertFalse(storage.exists("owners/" + account.ownerId + "/originals/cv.pdf"));
  }

  @Test
  @DisplayName("exclui só a versão pedida; outra versão ativa do mesmo owner continua intacta")
  void deletingOneVersionKeepsSiblingVersionIntact() {
    Account account = seedAccount();
    Seed toDelete = seedFullVersion(account.ownerId, 1, "SUPERSEDED");
    Seed toKeep = seedFullVersion(account.ownerId, 2, "ACTIVE");
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", toKeep.snapshotId, account.ownerId);
    authenticateAs(account);

    deletionService.delete(toDelete.versionId);

    assertEquals(0, count("select count(*) from base_versions where id = ?", toDelete.versionId));
    assertEquals(1, count("select count(*) from base_versions where id = ?", toKeep.versionId));
    assertEquals(1, count("select count(*) from facts where id = ?", toKeep.factId));
    assertEquals(toKeep.snapshotId, jdbc.queryForObject(
        "select active_snapshot_id from owners where id = ?", UUID.class, account.ownerId));
  }

  @Test
  @DisplayName("outra conta não pode excluir a versão")
  void deniesAccessToOtherAccount() {
    Account owner = seedAccount();
    Account stranger = seedAccount();
    Seed seed = seedFullVersion(owner.ownerId, 1, "ACTIVE");
    authenticateAs(stranger);

    assertThrows(AccessDeniedException.class, () -> deletionService.delete(seed.versionId));
    assertEquals(1, count("select count(*) from base_versions where id = ?", seed.versionId));
  }

  // ---------------------------------------------------------------- seed helpers

  private record Account(UUID ownerId, Authentication authentication) {
  }

  private record Seed(UUID documentId, UUID versionId, UUID runId, UUID pageId, UUID factId,
      UUID issueId, UUID decisionId, UUID snapshotId) {
  }

  private void authenticateAs(Account account) {
    SecurityContextHolder.getContext().setAuthentication(account.authentication);
  }

  private UUID idFor(String table, Seed seed) {
    return switch (table) {
      case "base_versions" -> seed.versionId;
      case "base_documents" -> seed.documentId;
      case "extraction_runs" -> seed.runId;
      case "gt_snapshots" -> seed.snapshotId;
      default -> throw new IllegalArgumentException(table);
    };
  }

  private Account seedAccount() {
    UUID userId = UUID.randomUUID();
    String email = "account-" + UUID.randomUUID() + "@example.com";
    jdbc.update("insert into users (id, email, password_hash, enabled, created_at) values (?, ?, 'x', true, now())",
        userId, email);
    UUID ownerId = UUID.randomUUID();
    jdbc.update("insert into owners (id, kind, user_id, revision) values (?, 'KIND_ACCOUNT', ?, 0)", ownerId, userId);
    return new Account(ownerId, new UsernamePasswordAuthenticationToken(email, "n/a", List.of()));
  }

  private Seed seedFullVersion(UUID ownerId, int number, String status) {
    UUID documentId = insertDocument(ownerId, "hash-" + UUID.randomUUID(),
        "owners/" + ownerId + "/originals/cv.pdf");
    UUID versionId = insertVersion(ownerId, documentId, number, status);
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
    UUID snapshotId = UUID.randomUUID();
    jdbc.update("insert into gt_snapshots (id, owner_id, base_version_id, snapshot_number, "
        + "approved_review_revision, content_hash, approved_at, schema_version) "
        + "values (?, ?, ?, ?, 0, 'ch', now(), 1)", snapshotId, ownerId, versionId, number);
    jdbc.update("insert into gt_snapshot_items (id, snapshot_id, source_fact_id, source_decision_id, record_id, "
        + "category, field_key, approved_value) values (?, ?, ?, ?, ?, 'identity', 'email', '\"a@b.c\"'::jsonb)",
        UUID.randomUUID(), snapshotId, factId, decisionId, recordId);

    return new Seed(documentId, versionId, runId, pageId, factId, issueId, decisionId, snapshotId);
  }

  private UUID insertGeneration(UUID ownerId, UUID snapshotId, UUID pdfObjectId, UUID markdownObjectId) {
    UUID templateId = UUID.randomUUID();
    jdbc.update("insert into cv_templates (id, name, version, content, content_hash, created_at) "
        + "values (?, ?, 1, 'content', 'ch', now())", templateId, "template-" + templateId);
    UUID jobId = UUID.randomUUID();
    jdbc.update("insert into job_postings (id, owner_id, source_kind, source_key, latest_version_number, "
        + "created_at, updated_at) values (?, ?, 'text', ?, 1, now(), now())",
        jobId, ownerId, "job-" + jobId);
    UUID jobVersionId = UUID.randomUUID();
    jdbc.update("insert into job_posting_versions (id, job_posting_id, version_number, content_hash, details, "
        + "analysis_status, created_at, last_checked_at) values (?, ?, 1, 'ch', '{}'::jsonb, 'unavailable', "
        + "now(), now())", jobVersionId, jobId);
    UUID generationId = UUID.randomUUID();
    jdbc.update("insert into cv_generations (id, owner_id, snapshot_id, job_posting_version_id, template_id, "
        + "input_hash, renderer_version, pipeline_version, preferences, document, ats, tailoring, "
        + "pdf_object_id, markdown_object_id, created_at) values (?, ?, ?, ?, ?, ?, 'test', 'test', "
        + "'{}'::jsonb, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, ?, ?, now())",
        generationId, ownerId, snapshotId, jobVersionId, templateId, "hash-" + generationId, pdfObjectId,
        markdownObjectId);
    return generationId;
  }

  private UUID insertObject(UUID ownerId, String key, String mime) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into stored_objects (id, owner_id, purpose, storage_key, content_hash, mime_type, "
        + "size_bytes, deletion_status) values (?, ?, 'TEST', ?, 'h', ?, 3, 'ACTIVE')",
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

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }
}
