package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tailorly.api.job.dto.CreateJobPostingRequest;
import com.tailorly.api.job.dto.JobPostingVersionResponse;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.session.AnonymousDataPurgeService;
import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.session.AnonymousSessionService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Exercita no PostgreSQL real a migração V8, o versionamento, a análise por IA (servidor falso), o
 * limite anônimo, o isolamento entre owners e a purga. Roda só com {@code TAILORLY_TEST_DATABASE_URL}.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.anonymous.cleanup-enabled=false",
    "tailorly.anonymous.max-job-postings=2",
    "tailorly.anonymous.max-extractions=2",
    "tailorly.anonymous.max-active-sessions=1000000",
    "tailorly.interpretation.provider=remote",
    "tailorly.interpretation.remote.model=test-model",
    "tailorly.interpretation.remote.api-key=key",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class JobPostingPersistenceIntegrationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final AtomicReference<String> AI_CONTENT = new AtomicReference<>();
  private static final AtomicReference<Boolean> AI_FAILS = new AtomicReference<>(false);
  private static HttpServer aiServer;

  @DynamicPropertySource
  static void aiEndpoint(DynamicPropertyRegistry registry) throws IOException {
    aiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    aiServer.createContext("/chat", exchange -> {
      if (AI_FAILS.get()) {
        exchange.sendResponseHeaders(500, -1);
        exchange.close();
        return;
      }
      byte[] body = MAPPER.writeValueAsBytes(
          Map.of("choices", List.of(Map.of("message", Map.of("content", AI_CONTENT.get())))));
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    aiServer.start();
    registry.add("tailorly.interpretation.remote.endpoint",
        () -> "http://127.0.0.1:" + aiServer.getAddress().getPort() + "/chat");
  }

  @AfterAll
  static void stopAi() {
    aiServer.stop(0);
  }

  static final Map<String, String> PAGES = new ConcurrentHashMap<>();

  @TestConfiguration
  static class Fakes {
    @Bean
    @Primary
    JobPageFetcher fakeFetcher() {
      return url -> {
        String html = PAGES.get(url);
        if (html == null)
          throw new IllegalStateException("no fake page for " + url);
        return new FetchedPage(Jsoup.parse(html, url), url);
      };
    }

    @Bean
    @Primary
    ObjectStorage fakeStorage() {
      return new ObjectStorage() {
        @Override public byte[] get(String key) { return null; }
        @Override public void put(String key, byte[] content, String mimeType) { }
        @Override public boolean exists(String key) { return false; }
        @Override public void delete(String key) { }
        @Override public void deleteByPrefix(String prefix) { }
      };
    }
  }

  @Autowired JobPostingService service;
  @Autowired AnonymousSessionService sessions;
  @Autowired AnonymousDataPurgeService purge;
  @Autowired JdbcTemplate jdbc;

  private UUID ownerId;

  @BeforeEach
  void setup() {
    PAGES.clear();
    AI_FAILS.set(false);
    AI_CONTENT.set("""
        {"seniority":null,"seniorityEvidence":null,
         "items":[{"id":1,"keep":true,"category":"technology","technologies":["Java"]}],
         "requirements":[
          {"text":"Java","importance":"required","category":"technology","technologies":["Java"],
           "evidence":"Experiência com Java."}],"technologies":["Java"]}
        """);
    var issued = sessions.issue();
    ownerId = issued.ownerId();
    SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
        new AnonymousPrincipal(issued.sessionId(), ownerId, issued.expiresAt())));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    purge.purgeOwner(ownerId);
  }

  private static String page(String job, String expiresAt) {
    return "<html><body><script id=\"__NEXT_DATA__\" type=\"application/json\">{\"props\":{\"pageProps\":{\"job\":"
        + "{\"id\":77,\"name\":\"Dev\",\"status\":\"published\",\"description\":\"<p>" + job + "</p>\","
        + "\"prerequisites\":\"Experiência com Java.\",\"registerEndDate\":\"" + expiresAt + "\","
        + "\"careerPage\":{\"name\":\"Acme\"}}}}}</script></body></html>";
  }

  private JobPostingVersionResponse create(String url) {
    return service.create(new CreateJobPostingRequest(ownerId, url, null, null, null)).version();
  }

  @Test
  void storesVersionsAndReusesIdenticalContent() {
    PAGES.put("https://acme.gupy.io/jobs/77?jobBoardSource=a", page("Primeira", "2999-01-01"));
    PAGES.put("https://acme.gupy.io/jobs/77?jobBoardSource=b", page("Primeira", "2999-01-01"));

    var first = service.create(new CreateJobPostingRequest(ownerId, "https://acme.gupy.io/jobs/77?jobBoardSource=a", null, null, null));
    var again = service.create(new CreateJobPostingRequest(ownerId, "https://acme.gupy.io/jobs/77?jobBoardSource=b", null, null, null));

    assertTrue(first.created());
    assertFalse(again.created());
    assertTrue(again.version().reused());
    assertEquals(first.version().id(), again.version().id());
    assertEquals(1, again.version().versionNumber());
    assertEquals("completed", first.version().analysisStatus());
    assertTrue(first.version().analysis().requirements().getFirst().text().contains("Java"));
    assertEquals(1, jdbc.queryForObject("select count(*) from job_postings where owner_id = ?", Integer.class, ownerId));
    assertEquals(1, jdbc.queryForObject("select count(*) from job_posting_versions", Integer.class));

    PAGES.put("https://acme.gupy.io/jobs/77?jobBoardSource=a", page("Segunda, texto mudou", "2999-01-01"));
    var changed = service.create(new CreateJobPostingRequest(ownerId, "https://acme.gupy.io/jobs/77?jobBoardSource=a", null, null, null));

    assertTrue(changed.created());
    assertEquals(2, changed.version().versionNumber());
    assertNotEquals(first.version().contentHash(), changed.version().contentHash());
    var detail = service.findById(changed.version().jobPostingId());
    assertEquals(2, detail.posting().latestVersionNumber());
    assertEquals(List.of(2, 1), detail.versions().stream().map(v -> v.versionNumber()).toList());
    // A versão antiga continua intacta.
    assertTrue(service.findVersion(changed.version().jobPostingId(), 1).details().descriptionText().contains("Primeira"));
  }

  @Test
  void expiryIsRecomputedWhenReadingAStoredVersion() {
    PAGES.put("https://acme.gupy.io/jobs/77", page("Texto", "2020-01-01"));

    var version = create("https://acme.gupy.io/jobs/77");

    assertEquals(true, version.details().expired());
    assertTrue(version.details().warnings().contains("expired"));
    assertEquals(true, service.findByOwner(ownerId).getFirst().expired());
  }

  @Test
  void aiFailureStillSavesThePostingAndCanBeRetried() {
    PAGES.put("https://acme.gupy.io/jobs/77", page("Texto", "2999-01-01"));
    AI_FAILS.set(true);

    var failed = create("https://acme.gupy.io/jobs/77");
    assertEquals("failed", failed.analysisStatus());
    assertEquals(null, failed.analysis());

    AI_FAILS.set(false);
    var retried = service.reanalyze(failed.jobPostingId(), 1);
    assertEquals("completed", retried.analysisStatus());
    assertEquals(1, retried.analysis().requirements().size());
  }

  @Test
  void pastedTextIsStoredAndDeduplicated() {
    var request = new CreateJobPostingRequest(ownerId, null, "Dev Backend\nRequisitos\nExperiência com Java.", null, "Acme");

    var first = service.create(request);
    var second = service.create(request);

    assertTrue(first.created());
    assertFalse(second.created());
    assertEquals("text", service.findByOwner(ownerId).getFirst().sourceKind());
    assertEquals("Acme", service.findByOwner(ownerId).getFirst().companyName());
  }

  @Test
  void requiresExactlyOneInput() {
    assertThrows(BadRequestException.class, () -> service.create(new CreateJobPostingRequest(ownerId, null, null, null, null)));
    assertThrows(BadRequestException.class, () -> service.create(new CreateJobPostingRequest(ownerId, "https://a.test/", "texto", null, null)));
  }

  @Test
  void anonymousOwnersAreCappedOnPostingsAndAnalyses() {
    for (int i = 1; i <= 2; i++) {
      var result = service.create(new CreateJobPostingRequest(ownerId, null, "Vaga " + i + "\nExperiência com Java.", null, null));
      assertEquals("completed", result.version().analysisStatus());
    }
    // O limite de processamentos (2) já foi consumido: a vaga é gravada, a análise fica pendente.
    var third = assertThrows(TooManyRequestsException.class,
        () -> service.create(new CreateJobPostingRequest(ownerId, null, "Vaga 3\nExperiência com Java.", null, null)));
    assertEquals("error.anonymous.job_limit_reached", third.getMessage());

    var reread = service.create(new CreateJobPostingRequest(ownerId, null, "Vaga 1\nExperiência com Java.", null, null));
    assertFalse(reread.created());
    assertEquals(2, jdbc.queryForObject("select count(*) from job_postings where owner_id = ?", Integer.class, ownerId));
  }

  @Test
  void analysisQuotaExceededIsRecordedWithoutLosingThePosting() {
    jdbc.update("update anonymous_sessions set extractions_count = 2 where owner_id = ?", ownerId);

    var result = service.create(new CreateJobPostingRequest(ownerId, null, "Vaga\nExperiência com Java.", null, null));

    assertEquals("quota_exceeded", result.version().analysisStatus());
    assertEquals(1, jdbc.queryForObject("select count(*) from job_postings where owner_id = ?", Integer.class, ownerId));
  }

  @Test
  void otherSessionsCannotReadOrDeleteThePosting() {
    var version = service.create(new CreateJobPostingRequest(ownerId, null, "Vaga\nExperiência com Java.", null, null)).version();
    var other = sessions.issue();
    try {
      SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
          new AnonymousPrincipal(other.sessionId(), other.ownerId(), Instant.now().plusSeconds(60))));

      assertThrows(AccessDeniedException.class, () -> service.findById(version.jobPostingId()));
      assertThrows(AccessDeniedException.class, () -> service.delete(version.jobPostingId()));
      assertThrows(AccessDeniedException.class, () -> service.findByOwner(ownerId));
    } finally {
      purge.purgeOwner(other.ownerId());
    }
  }

  @Test
  void deleteAndPurgeRemoveEveryVersion() {
    var version = service.create(new CreateJobPostingRequest(ownerId, null, "Vaga\nExperiência com Java.", null, null)).version();

    service.delete(version.jobPostingId());
    assertEquals(0, jdbc.queryForObject("select count(*) from job_postings where owner_id = ?", Integer.class, ownerId));
    assertEquals(0, jdbc.queryForObject("select count(*) from job_posting_versions where job_posting_id = ?", Integer.class, version.jobPostingId()));

    service.create(new CreateJobPostingRequest(ownerId, null, "Outra\nExperiência com Java.", null, null));
    purge.purgeOwner(ownerId);
    assertEquals(0, jdbc.queryForObject("select count(*) from job_postings where owner_id = ?", Integer.class, ownerId));
    assertEquals(0, jdbc.queryForObject("select count(*) from owners where id = ?", Integer.class, ownerId));
  }

  private record Account(UUID ownerId, UsernamePasswordAuthenticationToken authentication) {
  }

  private Account seedAccount() {
    UUID userId = UUID.randomUUID();
    String email = "account-" + UUID.randomUUID() + "@example.com";
    jdbc.update("insert into users (id, email, password_hash, enabled, created_at) values (?, ?, 'x', true, now())",
        userId, email);
    UUID accountOwner = UUID.randomUUID();
    jdbc.update("insert into owners (id, kind, user_id, revision) values (?, 'KIND_ACCOUNT', ?, 0)", accountOwner, userId);
    return new Account(accountOwner, new UsernamePasswordAuthenticationToken(email, "n/a", List.of()));
  }

  private void cleanAccount(Account account) {
    jdbc.update("delete from job_posting_versions where job_posting_id in (select id from job_postings where owner_id = ?)", account.ownerId());
    jdbc.update("delete from job_postings where owner_id = ?", account.ownerId());
    jdbc.update("delete from owners where id = ?", account.ownerId());
    jdbc.update("delete from users where email = ?", account.authentication().getName());
  }

  @Test
  void accountOwnersCreateReadListAndDeleteTheirPostings() {
    Account account = seedAccount();
    try {
      SecurityContextHolder.getContext().setAuthentication(account.authentication());

      var version = service.create(new CreateJobPostingRequest(account.ownerId(), null, "Vaga\nExperiência com Java.", null, null)).version();
      assertEquals("completed", version.analysisStatus());
      assertEquals(1, service.findByOwner(account.ownerId()).size());
      assertEquals(version.id(), service.findById(version.jobPostingId()).latest().id());
      assertEquals(version.id(), service.findVersion(version.jobPostingId(), 1).id());
      assertEquals("completed", service.reanalyze(version.jobPostingId(), 1).analysisStatus());

      service.delete(version.jobPostingId());
      assertEquals(0, service.findByOwner(account.ownerId()).size());
    } finally {
      cleanAccount(account);
    }
  }

  @Test
  void anAnonymousSessionCannotTouchAnAccountPostingAndViceVersa() {
    Account account = seedAccount();
    try {
      SecurityContextHolder.getContext().setAuthentication(account.authentication());
      var accountPosting = service.create(new CreateJobPostingRequest(account.ownerId(), null, "Vaga conta\nExperiência com Java.", null, null)).version();

      // Sessão anônima (credencial do @BeforeEach) contra o posting da conta.
      var issued = sessions.issue();
      try {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
            new AnonymousPrincipal(issued.sessionId(), issued.ownerId(), issued.expiresAt())));
        var denied = assertThrows(AccessDeniedException.class, () -> service.findById(accountPosting.jobPostingId()));
        assertEquals("error.owner.access_denied", denied.getMessage());
        assertThrows(AccessDeniedException.class, () -> service.create(
            new CreateJobPostingRequest(account.ownerId(), null, "Outra\nJava", null, null)));
        var visitorPosting = service.create(new CreateJobPostingRequest(issued.ownerId(), null, "Vaga visitante\nExperiência com Java.", null, null)).version();

        // A conta não lê o posting do visitante com a credencial errada.
        SecurityContextHolder.getContext().setAuthentication(account.authentication());
        assertThrows(AccessDeniedException.class, () -> service.findById(visitorPosting.jobPostingId()));
        assertThrows(AccessDeniedException.class, () -> service.findByOwner(issued.ownerId()));
      } finally {
        purge.purgeOwner(issued.ownerId());
      }
    } finally {
      cleanAccount(account);
    }
  }
}
