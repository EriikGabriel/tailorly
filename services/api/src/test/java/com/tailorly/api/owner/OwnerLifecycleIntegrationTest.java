package com.tailorly.api.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.owner.dto.OwnerResponse;
import com.tailorly.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercita no PostgreSQL real a desativação do Ground Truth ativo: diferente da exclusão de
 * versão, nada deve ser apagado — só {@code owners.active_snapshot_id} zera e a versão que
 * estava {@code ACTIVE} vira {@code DEACTIVATED}. Roda só com {@code TAILORLY_TEST_DATABASE_URL}
 * definido.
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
class OwnerLifecycleIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired OwnerService ownerService;

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("desativa o snapshot ativo sem apagar nada: fatos, pendências e snapshot continuam no banco")
  void deactivatesWithoutDeletingData() {
    Account account = seedAccountWithActiveSnapshot();
    authenticateAs(account);

    OwnerResponse response = ownerService.deactivateActiveSnapshot(account.ownerId);

    assertNull(response.activeSnapshotId());
    assertNull(jdbc.queryForObject("select active_snapshot_id from owners where id = ?",
        UUID.class, account.ownerId));
    assertEquals("DEACTIVATED", jdbc.queryForObject(
        "select status from base_versions where id = ?", String.class, account.versionId));
    assertEquals(1, count("select count(*) from gt_snapshots where id = ?", account.snapshotId));
    assertEquals(1, count("select count(*) from facts where id = ?", account.factId));
    assertEquals(1, jdbc.queryForObject(
        "select revision from owners where id = ?", Integer.class, account.ownerId));
  }

  @Test
  @DisplayName("desativar sem snapshot ativo falha")
  void failsWithoutActiveSnapshot() {
    Account account = seedAccount();
    authenticateAs(account);

    assertThrows(BadRequestException.class, () -> ownerService.deactivateActiveSnapshot(account.ownerId));
  }

  // ---------------------------------------------------------------- seed helpers

  private record Account(UUID ownerId, Authentication authentication, UUID versionId, UUID snapshotId,
      UUID factId) {
  }

  private void authenticateAs(Account account) {
    SecurityContextHolder.getContext().setAuthentication(account.authentication);
  }

  private Account seedAccount() {
    UUID userId = UUID.randomUUID();
    String email = "account-" + UUID.randomUUID() + "@example.com";
    jdbc.update("insert into users (id, email, password_hash, enabled, created_at) values (?, ?, 'x', true, now())",
        userId, email);
    UUID ownerId = UUID.randomUUID();
    jdbc.update("insert into owners (id, kind, user_id, revision) values (?, 'KIND_ACCOUNT', ?, 0)", ownerId, userId);
    return new Account(ownerId, new UsernamePasswordAuthenticationToken(email, "n/a", List.of()), null, null, null);
  }

  private Account seedAccountWithActiveSnapshot() {
    Account account = seedAccount();
    UUID objectId = UUID.randomUUID();
    jdbc.update("insert into stored_objects (id, owner_id, purpose, storage_key, content_hash, mime_type, "
        + "size_bytes, deletion_status) values (?, ?, 'TEST', ?, 'h', 'application/pdf', 3, 'ACTIVE')",
        objectId, account.ownerId, "owners/" + account.ownerId + "/originals/cv.pdf");
    UUID documentId = UUID.randomUUID();
    jdbc.update("insert into base_documents (id, owner_id, original_object_id, content_hash, original_filename, "
        + "page_count, created_at) values (?, ?, ?, 'hash', 'cv.pdf', 1, now())",
        documentId, account.ownerId, objectId);
    UUID versionId = UUID.randomUUID();
    jdbc.update("insert into base_versions (id, owner_id, document_id, version_number, status, "
        + "review_revision, created_at, temporal_review_required) values (?, ?, ?, 1, 'ACTIVE', 0, now(), false)",
        versionId, account.ownerId, documentId);
    UUID recordId = UUID.randomUUID();
    jdbc.update("insert into fact_records (id, base_version_id, category, display_order) values (?, ?, 'identity', 0)",
        recordId, versionId);
    UUID factId = UUID.randomUUID();
    jdbc.update("insert into facts (id, record_id, field_key, extracted_value, reviewed_value, origin, "
        + "review_state) values (?, ?, 'email', '\"a@b.c\"'::jsonb, '\"a@b.c\"'::jsonb, 'ai', 'confirmado')",
        factId, recordId);
    UUID snapshotId = UUID.randomUUID();
    jdbc.update("insert into gt_snapshots (id, owner_id, base_version_id, snapshot_number, "
        + "approved_review_revision, content_hash, approved_at, schema_version) "
        + "values (?, ?, ?, 1, 0, 'ch', now(), 1)", snapshotId, account.ownerId, versionId);
    jdbc.update("update owners set active_snapshot_id = ? where id = ?", snapshotId, account.ownerId);
    return new Account(account.ownerId, account.authentication, versionId, snapshotId, factId);
  }

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }
}
