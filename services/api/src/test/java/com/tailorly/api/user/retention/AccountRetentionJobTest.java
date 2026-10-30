package com.tailorly.api.user.retention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.tailorly.api.objects.storage.ObjectStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercita no PostgreSQL real a política de retenção de contas: uma conta desativada há mais
 * tempo que o período configurado perde os dados pessoais do owner, mas a linha de
 * {@code users}/{@code owners} continua existindo. Roda só com {@code TAILORLY_TEST_DATABASE_URL}
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
    "tailorly.account.retention-period=P30D",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class AccountRetentionJobTest {
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
  @Autowired AccountDataRetentionService retention;
  @Autowired com.tailorly.api.user.UserRepository users;
  @Autowired AccountRetentionProperties properties;

  @Test
  @DisplayName("purga contas desativadas há mais que o período de retenção, mantendo users/owners")
  void purgesAccountsPastRetentionPeriod() {
    Account overdue = seedAccount(Instant.now().minus(properties.retentionPeriod()).minusSeconds(3600));
    Account recent = seedAccount(Instant.now().minusSeconds(60));
    AccountRetentionJob job = new AccountRetentionJob(users, retention, properties);

    int purged = job.purgeEligible(Instant.now());

    assertTrue(purged >= 1);
    assertEquals(1, count("select count(*) from users where id = ?", overdue.userId));
    assertEquals(1, count("select count(*) from owners where id = ?", overdue.ownerId));
    assertEquals(0, count("select count(*) from base_versions where owner_id = ?", overdue.ownerId));
    assertEquals(0, count("select count(*) from base_documents where owner_id = ?", overdue.ownerId));
    assertFalse(storage.exists("owners/" + overdue.ownerId + "/originals/cv.pdf"));
    assertNotNull(jdbc.queryForObject("select data_purged_at from users where id = ?",
        Timestamp.class, overdue.userId));

    assertEquals(1, count("select count(*) from base_versions where owner_id = ?", recent.ownerId));
    assertNull(jdbc.queryForObject("select data_purged_at from users where id = ?",
        Timestamp.class, recent.userId));
  }

  @Test
  @DisplayName("não processa de novo uma conta já purgada")
  void skipsAlreadyPurgedAccounts() {
    Account overdue = seedAccount(Instant.now().minus(properties.retentionPeriod()).minusSeconds(3600));
    jdbc.update("update users set data_purged_at = now() where id = ?", overdue.userId);
    AccountRetentionJob job = new AccountRetentionJob(users, retention, properties);

    int purged = job.purgeEligible(Instant.now());

    assertEquals(0, purged);
  }

  private record Account(UUID userId, UUID ownerId) {
  }

  private Account seedAccount(Instant disabledAt) {
    UUID userId = UUID.randomUUID();
    String email = "retained-" + UUID.randomUUID() + "@example.com";
    jdbc.update("insert into users (id, email, password_hash, enabled, disabled_at, created_at) "
        + "values (?, ?, 'x', false, ?, now())", userId, email, Timestamp.from(disabledAt));
    UUID ownerId = UUID.randomUUID();
    jdbc.update("insert into owners (id, kind, user_id, revision) values (?, 'KIND_ACCOUNT', ?, 0)", ownerId, userId);

    UUID objectId = UUID.randomUUID();
    String key = "owners/" + ownerId + "/originals/cv.pdf";
    jdbc.update("insert into stored_objects (id, owner_id, purpose, storage_key, content_hash, mime_type, "
        + "size_bytes, deletion_status) values (?, ?, 'TEST', ?, 'h', 'application/pdf', 3, 'ACTIVE')",
        objectId, ownerId, key);
    storage.objects.put(key, "bytes".getBytes());
    UUID documentId = UUID.randomUUID();
    jdbc.update("insert into base_documents (id, owner_id, original_object_id, content_hash, original_filename, "
        + "page_count, created_at) values (?, ?, ?, 'hash', 'cv.pdf', 1, now())", documentId, ownerId, objectId);
    UUID versionId = UUID.randomUUID();
    jdbc.update("insert into base_versions (id, owner_id, document_id, version_number, status, "
        + "review_revision, created_at, temporal_review_required) values (?, ?, ?, 1, 'ACTIVE', 0, now(), false)",
        versionId, ownerId, documentId);

    return new Account(userId, ownerId);
  }

  private int count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Integer.class, args);
  }
}
