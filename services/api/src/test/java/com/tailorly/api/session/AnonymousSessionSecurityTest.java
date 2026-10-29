package com.tailorly.api.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.base.document.BaseDocumentController;
import com.tailorly.api.base.document.BaseDocumentService;
import com.tailorly.api.base.document.BaseDocumentUploadService;
import com.tailorly.api.session.dto.AnonymousSessionStatusResponse;
import com.tailorly.api.session.dto.IssuedAnonymousSessionResponse;
import com.tailorly.api.shared.config.SecurityConfiguration;
import com.tailorly.api.shared.security.CurrentAccountController;
import com.tailorly.api.shared.security.CurrentAccountService;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import com.tailorly.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AnonymousSessionController.class, CurrentAccountController.class,
    BaseDocumentController.class})
@Import({SecurityConfiguration.class, LocalizedSecurityErrorHandler.class,
    AnonymousSessionConfiguration.class})
@TestPropertySource(properties = {
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.jwt.issuer=http://localhost:8080",
    "tailorly.security.jwt.ttl=PT1H",
    "tailorly.anonymous.session-ttl=PT1H",
    "tailorly.anonymous.max-file-bytes=1000",
    "tailorly.anonymous.max-pages=3",
    "tailorly.anonymous.max-uploads=3",
    "tailorly.anonymous.max-extractions=3",
    "tailorly.anonymous.max-generations=3",
    "tailorly.anonymous.max-job-postings=10",
    "tailorly.anonymous.requests-per-minute=10",
    "tailorly.anonymous.sessions-per-ip-per-hour=2",
    "tailorly.anonymous.max-active-sessions=100",
    "tailorly.anonymous.cleanup-interval=PT5M",
    "tailorly.anonymous.cleanup-enabled=false"
})
class AnonymousSessionSecurityTest {
  private static final String VALID = "tlr_anon_valid";
  private static final String EXPIRED = "tlr_anon_expired";

  @Autowired MockMvc mvc;
  @Autowired FixedWindowRateLimiter anonymousRequestLimiter;

  @MockitoBean UserRepository userRepository;
  @MockitoBean CurrentAccountService currentAccountService;
  @MockitoBean AnonymousSessionService sessionService;
  @MockitoBean AnonymousMigrationService migrationService;
  @MockitoBean BaseDocumentService baseDocumentService;
  @MockitoBean BaseDocumentUploadService uploadService;
  @MockitoBean AnonymousSessionRepository sessionRepository;
  @MockitoBean AnonymousDataPurgeService purgeService;

  private final UUID ownerId = UUID.randomUUID();
  private final AnonymousPrincipal principal =
      new AnonymousPrincipal(UUID.randomUUID(), ownerId, Instant.now().plusSeconds(3600));

  @BeforeEach
  void setup() {
    when(sessionService.authenticate(VALID)).thenReturn(Optional.of(principal));
    when(sessionService.authenticate(EXPIRED)).thenReturn(Optional.empty());
  }

  @Test
  void issuingASessionIsPublicButLimitedPerIp() throws Exception {
    when(sessionService.issue()).thenReturn(new IssuedAnonymousSessionResponse(
        principal.sessionId(), ownerId, "tlr_anon_new", principal.expiresAt()));

    for (int i = 0; i < 2; i++) {
      mvc.perform(post("/v1/anonymous-sessions").with(request -> {
            request.setRemoteAddr("203.0.113.9");
            return request;
          }))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.token").value("tlr_anon_new"))
          .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
    }
    mvc.perform(post("/v1/anonymous-sessions").with(request -> {
          request.setRemoteAddr("203.0.113.9");
          return request;
        }))
        .andExpect(status().isTooManyRequests());
    // Outro IP tem a própria cota.
    mvc.perform(post("/v1/anonymous-sessions").with(request -> {
          request.setRemoteAddr("203.0.113.10");
          return request;
        }))
        .andExpect(status().isCreated());
  }

  @Test
  void validTokenAuthenticatesOnTheCurrentSessionEndpoint() throws Exception {
    when(sessionService.status(principal)).thenReturn(AnonymousSessionStatusResponse.builder()
        .sessionId(principal.sessionId())
        .ownerId(ownerId)
        .expiresAt(principal.expiresAt())
        .migratable(false)
        .usage(new AnonymousSessionStatusResponse.Usage(1, 0, 0))
        .limits(new AnonymousSessionStatusResponse.Limits(1000, 3, 3, 3, 3))
        .build());

    mvc.perform(get("/v1/anonymous-sessions/current").header("Authorization", "Bearer " + VALID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
        .andExpect(jsonPath("$.usage.uploads").value(1))
        .andExpect(jsonPath("$.limits.maxPages").value(3));
  }

  @Test
  void invalidOrExpiredTokenIsRejectedWith401() throws Exception {
    mvc.perform(get("/v1/anonymous-sessions/current").header("Authorization", "Bearer " + EXPIRED))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/base-documents/owner/" + ownerId).header("Authorization", "Bearer " + EXPIRED))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingCredentialsAreRejectedOnDataRoutes() throws Exception {
    mvc.perform(get("/v1/base-documents/owner/" + ownerId)).andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/anonymous-sessions/current")).andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousSessionReachesBaseDocumentRoutes() throws Exception {
    when(baseDocumentService.findAllByOwnerId(ownerId)).thenReturn(List.of());

    mvc.perform(get("/v1/base-documents/owner/" + ownerId).header("Authorization", "Bearer " + VALID))
        .andExpect(status().isOk());
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .multipart("/v1/base-documents/upload")
            .file(new MockMultipartFile("file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-".getBytes()))
            .param("ownerId", ownerId.toString())
            .header("Authorization", "Bearer " + VALID))
        .andExpect(status().isCreated());
  }

  @Test
  void anonymousSessionCannotReachAccountOnlyRoutes() throws Exception {
    mvc.perform(get("/v1/me").header("Authorization", "Bearer " + VALID))
        .andExpect(status().isForbidden());
    mvc.perform(get("/v1/ai-preferences").header("Authorization", "Bearer " + VALID))
        .andExpect(status().isForbidden());
    mvc.perform(post("/v1/anonymous-sessions/migrate")
            .header("Authorization", "Bearer " + VALID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionToken\":\"" + VALID + "\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/v1/anonymous-sessions/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + VALID))
        .andExpect(status().isForbidden());
    mvc.perform(get("/v1/stored-objects/owner/" + ownerId).header("Authorization", "Bearer " + VALID))
        .andExpect(status().isForbidden());
    // O alias legado /api/* não faz parte do acesso anônimo.
    mvc.perform(get("/api/base-documents/owner/" + ownerId).header("Authorization", "Bearer " + VALID))
        .andExpect(status().isForbidden());
  }

  @Test
  void endingTheSessionPurgesItsData() throws Exception {
    mvc.perform(delete("/v1/anonymous-sessions/current").header("Authorization", "Bearer " + VALID))
        .andExpect(status().isNoContent());

    org.mockito.Mockito.verify(sessionService).destroy(principal);
  }

  @Test
  void requestsPerSessionAreRateLimited() throws Exception {
    when(baseDocumentService.findAllByOwnerId(any())).thenReturn(List.of());
    anonymousRequestLimiter.tryAcquire(principal.sessionId().toString());

    for (int i = 0; i < 9; i++) {
      mvc.perform(get("/v1/base-documents/owner/" + ownerId).header("Authorization", "Bearer " + VALID))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/v1/base-documents/owner/" + ownerId).header("Authorization", "Bearer " + VALID))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }
}
