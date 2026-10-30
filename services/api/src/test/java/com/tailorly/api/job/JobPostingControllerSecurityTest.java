package com.tailorly.api.job;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingVersionResponse;
import com.tailorly.api.job.reader.JobPostingExtractionService;
import com.tailorly.api.session.AnonymousDataPurgeService;
import com.tailorly.api.session.AnonymousMigrationService;
import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionConfiguration;
import com.tailorly.api.session.AnonymousSessionRepository;
import com.tailorly.api.session.AnonymousSessionService;
import com.tailorly.api.shared.config.SecurityConfiguration;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import com.tailorly.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobPostingController.class)
@Import({SecurityConfiguration.class, LocalizedSecurityErrorHandler.class, AnonymousSessionConfiguration.class})
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
    "tailorly.anonymous.requests-per-minute=100",
    "tailorly.anonymous.sessions-per-ip-per-hour=2",
    "tailorly.anonymous.max-active-sessions=100",
    "tailorly.anonymous.cleanup-interval=PT5M",
    "tailorly.anonymous.cleanup-enabled=false"
})
class JobPostingControllerSecurityTest {
  private static final String ANON = "tlr_anon_valid";
  private static final String BODY = "{\"url\":\"https://acme.gupy.io/jobs/1\"}";

  @Autowired MockMvc mvc;

  @MockitoBean JobPostingExtractionService service;
  @MockitoBean JobPostingService postingService;
  @MockitoBean UserRepository userRepository;
  @MockitoBean AnonymousSessionService sessionService;
  @MockitoBean AnonymousMigrationService migrationService;
  @MockitoBean AnonymousSessionRepository sessionRepository;
  @MockitoBean AnonymousDataPurgeService purgeService;

  @BeforeEach
  void setup() {
    AnonymousPrincipal principal =
        new AnonymousPrincipal(UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(3600));
    when(sessionService.authenticate(ANON)).thenReturn(Optional.of(principal));
  }

  private static JobPostingResponse response() {
    return JobPostingResponse.builder().platform("gupy").title("Dev").requirements(List.of("Java"))
        .warnings(List.of()).build();
  }

  @Test
  void anonymousSessionCanExtractAJob() throws Exception {
    when(service.extract("https://acme.gupy.io/jobs/1")).thenReturn(response());

    mvc.perform(post("/v1/job-postings/extract").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.platform").value("gupy"))
        .andExpect(jsonPath("$.requirements[0]").value("Java"));
  }

  @Test
  void requestsWithoutCredentialsAreRejectedBeforeFetching() throws Exception {
    mvc.perform(post("/v1/job-postings/extract").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());

    verify(service, never()).extract(anyString());
  }

  @Test
  void blankAndOversizedUrlsFailValidation() throws Exception {
    for (String body : new String[] {"{\"url\":\"  \"}", "{}", "{\"url\":\"https://a.test/" + "x".repeat(2100) + "\"}"}) {
      mvc.perform(post("/v1/job-postings/extract").header("Authorization", "Bearer " + ANON)
              .contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verify(service, never()).extract(anyString());
  }

  @Test
  void blockedLinksAreBadRequestsAndUnreadablePagesAreUnprocessable() throws Exception {
    when(service.extract("https://acme.gupy.io/jobs/1"))
        .thenThrow(new BadRequestException(ErrorCodes.Job.URL_NOT_ALLOWED));

    mvc.perform(post("/v1/job-postings/extract").header("Authorization", "Bearer " + ANON)
            .header("Accept-Language", "pt-BR").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Este link aponta para um endereço que não pode ser acessado"));

    doThrow(new UnprocessableRequestException(ErrorCodes.Job.NO_CONTENT))
        .when(service).extract("https://acme.gupy.io/jobs/1");

    mvc.perform(post("/v1/job-postings/extract").header("Authorization", "Bearer " + ANON)
            .header("Accept-Language", "pt-BR").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("Entidade não processável"))
        .andExpect(jsonPath("$.message").value(
            "Nenhum detalhe de vaga foi encontrado neste link. Cole o texto da vaga"));
  }

  @Test
  void createReturns201ForNewVersionsAnd200WhenReused() throws Exception {
    UUID owner = UUID.randomUUID();
    UUID posting = UUID.randomUUID();
    var version = new JobPostingVersionResponse(UUID.randomUUID(), posting, 1, "h", response(), null,
        "unavailable", null, Instant.now(), Instant.now(), false);
    String body = "{\"ownerId\":\"" + owner + "\",\"text\":\"Vaga\\nJava\"}";
    when(postingService.create(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new JobPostingService.CreateResult(version, true))
        .thenReturn(new JobPostingService.CreateResult(version, false));

    mvc.perform(post("/v1/job-postings").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.versionNumber").value(1));
    mvc.perform(post("/v1/job-postings").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
  }

  @Test
  void createRequiresAnOwnerAndCredentials() throws Exception {
    mvc.perform(post("/v1/job-postings").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Vaga\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/v1/job-postings").contentType(MediaType.APPLICATION_JSON)
            .content("{\"ownerId\":\"" + UUID.randomUUID() + "\",\"text\":\"Vaga\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteAndReadEndpointsAreReachableByAnonymousSessions() throws Exception {
    UUID id = UUID.randomUUID();

    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/v1/job-postings/" + id)
            .header("Authorization", "Bearer " + ANON))
        .andExpect(status().isNoContent());
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/job-postings/owner/" + id)
            .header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk());
    mvc.perform(post("/v1/job-postings/" + id + "/versions/1/analysis")
            .header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk());
  }
}
