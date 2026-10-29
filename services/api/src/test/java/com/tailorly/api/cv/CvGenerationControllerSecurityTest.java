package com.tailorly.api.cv;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.cv.dto.CvGenerationResponse;
import com.tailorly.api.session.AnonymousDataPurgeService;
import com.tailorly.api.session.AnonymousMigrationService;
import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionConfiguration;
import com.tailorly.api.session.AnonymousSessionRepository;
import com.tailorly.api.session.AnonymousSessionService;
import com.tailorly.api.shared.config.SecurityConfiguration;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ServiceUnavailableException;
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

@WebMvcTest({CvGenerationController.class, CvTemplateController.class})
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
class CvGenerationControllerSecurityTest {
  private static final String ANON = "tlr_anon_valid";

  @Autowired MockMvc mvc;

  @MockitoBean CvGenerationService service;
  @MockitoBean com.tailorly.api.cv.template.CvTemplateService templateService;
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

  private static CvGenerationResponse generation() {
    return new CvGenerationResponse(UUID.randomUUID(), UUID.randomUUID(),
        new CvGenerationResponse.GroundTruthSource(UUID.randomUUID(), 1, "h", UUID.randomUUID()),
        new CvGenerationResponse.JobPostingSource(UUID.randomUUID(), UUID.randomUUID(), 2, "jh", "Dev", "Acme"),
        new CvGenerationResponse.TemplateSource(UUID.randomUUID(), "classic", 1, false),
        com.tailorly.api.cv.model.CvPreferences.defaults(),
        new com.tailorly.api.cv.pipeline.CvAtsResult(null, List.of(), "unavailable"),
        "typst 0.13", List.of("java"), "completed", null, null, Instant.now(), false);
  }

  private static String body() {
    return "{\"ownerId\":\"" + UUID.randomUUID() + "\",\"jobPostingId\":\"" + UUID.randomUUID() + "\"}";
  }

  @Test
  void generateReturns201ForNewAnd200ForReusedGenerations() throws Exception {
    when(service.generate(any()))
        .thenReturn(new CvGenerationService.Result(generation(), true))
        .thenReturn(new CvGenerationService.Result(generation(), false));

    mvc.perform(post("/v1/cv/generate").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.job.versionNumber").value(2))
        .andExpect(jsonPath("$.template.name").value("classic"));
    mvc.perform(post("/v1/cv/generate").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isOk());
  }

  @Test
  void generateRequiresCredentialsAndRequiredFields() throws Exception {
    mvc.perform(post("/v1/cv/generate").contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/v1/cv/generate").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content("{\"ownerId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/v1/cv/generate").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ownerId\":\"" + UUID.randomUUID() + "\",\"jobPostingId\":\"" + UUID.randomUUID()
                + "\",\"jobVersionNumber\":0}"))
        .andExpect(status().isBadRequest());

    verify(service, never()).generate(any());
  }

  @Test
  void unavailableCompilerIs503WithLocalizedMessage() throws Exception {
    when(service.generate(any()))
        .thenThrow(new ServiceUnavailableException(ErrorCodes.CvGeneration.RENDERER_UNAVAILABLE));

    mvc.perform(post("/v1/cv/generate").header("Authorization", "Bearer " + ANON)
            .header("Accept-Language", "pt-BR").contentType(MediaType.APPLICATION_JSON).content(body()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("A geração de PDF está temporariamente indisponível"));
  }

  @Test
  void downloadSendsAnAttachmentThatIsNeverCached() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.download(id, "pdf")).thenReturn(
        new CvGenerationService.Download("%PDF-x".getBytes(StandardCharsets.UTF_8), "application/pdf",
            "curriculo-acme.pdf"));

    mvc.perform(get("/v1/cv/generations/" + id + "/download").header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"curriculo-acme.pdf\""))
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andExpect(content().bytes("%PDF-x".getBytes(StandardCharsets.UTF_8)));
    mvc.perform(get("/v1/cv/generations/" + id + "/download")).andExpect(status().isUnauthorized());
  }

  @Test
  void historyDetailAndDeleteAreReachableByAnonymousSessions() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.findByOwner(id)).thenReturn(List.of(generation()));

    mvc.perform(get("/v1/cv/generations/owner/" + id).header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].job.title").value("Dev"));
    mvc.perform(get("/v1/cv/generations/" + id).header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk());
    mvc.perform(delete("/v1/cv/generations/" + id).header("Authorization", "Bearer " + ANON))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/v1/cv/generations/" + id)).andExpect(status().isUnauthorized());
    verify(service).delete(id);
  }

  @Test
  void previewReturnsThePdfAndCompilerDiagnosticsAs422() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.preview(org.mockito.ArgumentMatchers.eq(id), any()))
        .thenReturn("%PDF-x".getBytes(StandardCharsets.UTF_8))
        .thenThrow(new com.tailorly.api.shared.exception.UnprocessableRequestException(
            ErrorCodes.CvGeneration.TEMPLATE_INVALID, List.of("error: unknown variable: x")));

    mvc.perform(post("/v1/cv/generations/" + id + "/preview").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"= oi\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(header().string("Cache-Control", "private, no-store"));
    mvc.perform(post("/v1/cv/generations/" + id + "/preview").header("Authorization", "Bearer " + ANON)
            .header("Accept-Language", "pt-BR").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"#x\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("Não foi possível compilar o template"))
        .andExpect(jsonPath("$.details[0]").value("error: unknown variable: x"));
    mvc.perform(post("/v1/cv/generations/" + id + "/preview").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/v1/cv/generations/" + id + "/preview").contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"= oi\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void templateRoutesAreReachableByAnonymousSessionsAndNeedCredentials() throws Exception {
    UUID owner = UUID.randomUUID();
    var template = new com.tailorly.api.cv.dto.CvTemplateResponse(UUID.randomUUID(), "custom", 1, true, "h", "= oi",
        Instant.now());
    when(templateService.current(owner)).thenReturn(template);
    when(templateService.save(any(), any())).thenReturn(template);

    mvc.perform(get("/v1/cv/templates/owner/" + owner + "/current").header("Authorization", "Bearer " + ANON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.custom").value(true));
    mvc.perform(post("/v1/cv/templates").header("Authorization", "Bearer " + ANON)
            .contentType(MediaType.APPLICATION_JSON).content("{\"ownerId\":\"" + owner + "\",\"content\":\"= oi\"}"))
        .andExpect(status().isCreated());
    mvc.perform(get("/v1/cv/templates/bundled").header("Authorization", "Bearer " + ANON)).andExpect(status().isOk());
    mvc.perform(get("/v1/cv/templates/bundled")).andExpect(status().isUnauthorized());
  }
}
