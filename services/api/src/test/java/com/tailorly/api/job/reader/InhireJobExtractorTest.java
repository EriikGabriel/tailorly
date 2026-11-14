package com.tailorly.api.job.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class InhireJobExtractorTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String ID = "1a963223-260a-4242-bbb7-f53dd801d760";
  private static final String URL = "https://acme.inhire.app/vagas/" + ID
      + "/desenvolvedor-de-software-full-stack-junior?source=linkedin";

  // Formato observado em GET /job-posts/public/pages/{id} (cabeçalho x-tenant), com texto sintético.
  private static final String BODY = """
      {"tenantName":"Acme","about":"<p><strong>A solução completa para a indústria.</strong></p><p>Prosperidade ao alcance de todos.</p>",
      "logo":"https://files.inhire.app/tenant-public-files/acme/logo.png","displayName":"Desenvolvedor de Software Full Stack Júnior",
      "description":"<p>Buscamos uma pessoa desenvolvedora para evoluir solu&ccedil;&otilde;es.</p><h2><strong>Hard Skills</strong></h2><ul><li>Conhecimentos em Node.js e TypeScript;</li><li>Vue e/ou React;</li></ul><h2><strong>Nossos benefícios</strong></h2><p>Plano de Saúde;</p><p>Auxílio home office;</p>",
      "status":"published","publishedAt":"2026-10-20T13:35:08.797Z","workplaceType":"Remote","location":"","contractType":[],"jobId":"%s"}
      """.formatted(ID);

  private static InhireJobExtractor extractorReturning(String body, AtomicReference<String[]> seen) {
    return new InhireJobExtractor((tenant, id) -> {
      if (seen != null)
        seen.set(new String[] {tenant, id});
      return body == null ? Optional.empty() : Optional.of(read(body));
    });
  }

  private static JsonNode read(String body) {
    return MAPPER.readTree(body);
  }

  @Test
  void recognizesInhireLinksOnly() {
    assertTrue(InhireJobExtractor.match(URL).isPresent());
    assertEquals("acme", InhireJobExtractor.match(URL).get().tenant());
    assertEquals(ID, InhireJobExtractor.match(URL).get().jobId());
    assertTrue(InhireJobExtractor.match("https://acme.inhire.app/vagas/" + ID).isPresent());
    assertFalse(InhireJobExtractor.match("https://acme.inhire.app/vagas").isPresent());
    assertFalse(InhireJobExtractor.match("https://acme.inhire.app/vagas/not-a-uuid").isPresent());
    assertFalse(InhireJobExtractor.match("https://api.inhire.app/vagas/" + ID).isPresent());
    assertFalse(InhireJobExtractor.match("https://inhire.app.evil.test/vagas/" + ID).isPresent());
    assertFalse(InhireJobExtractor.match("https://evilinhire.app/vagas/" + ID).isPresent());
    assertFalse(InhireJobExtractor.match("https://a:b@acme.inhire.app/vagas/" + ID).isPresent());
    assertFalse(InhireJobExtractor.match("ftp://acme.inhire.app/vagas/" + ID).isPresent());
  }

  @Test
  void queriesTheApiWithTenantAndJobIdFromTheLink() {
    AtomicReference<String[]> seen = new AtomicReference<>();
    extractorReturning(BODY, seen).extract(URL);

    assertEquals("acme", seen.get()[0]);
    assertEquals(ID, seen.get()[1]);
  }

  @Test
  void readsJobFieldsAndSections() {
    JobPostingResponse job = extractorReturning(BODY, null).extract(URL).orElseThrow();

    assertEquals("inhire", job.platform());
    assertEquals(ID, job.externalId());
    assertEquals("https://acme.inhire.app/vagas/" + ID, job.canonicalUrl());
    assertEquals("Desenvolvedor de Software Full Stack Júnior", job.title());
    assertEquals("remote", job.workModel());
    assertNull(job.location());
    assertNull(job.employmentType());
    assertEquals("Acme", job.company().name());
    assertEquals("https://acme.inhire.app", job.company().careerPageUrl());
    assertTrue(job.company().description().contains("Prosperidade"));
    assertTrue(job.descriptionText().contains("soluções"));
    assertEquals(2, job.requirements().size());
    assertTrue(job.requirements().get(0).contains("Node.js"));
    assertEquals(2, job.benefits().size());
    assertTrue(job.warnings().isEmpty());
  }

  @Test
  void flagsUnpublishedJobs() {
    JobPostingResponse job = extractorReturning(BODY.replace("\"published\"", "\"closed\""), null).extract(URL).orElseThrow();

    assertTrue(job.warnings().contains("job_not_published"));
  }

  @Test
  void ignoresNonInhireLinksAndMissingJobs() {
    assertTrue(extractorReturning(BODY, null).extract("https://acme.gupy.io/jobs/1").isEmpty());
    assertTrue(extractorReturning(null, null).extract(URL).isEmpty());
  }

  @Test
  void serviceUsesTheApiInsteadOfTheSpaShell() {
    JobPageFetcher fetcher = url -> {
      throw new AssertionError("a página da InHire não deve ser baixada");
    };
    var service = new JobPostingExtractionService(fetcher, extractorReturning(BODY, null),
        new GupyJobExtractor(MAPPER), new JsonLdJobExtractor(), new GenericHtmlJobExtractor(), new PastedTextJobExtractor());

    JobPostingResponse job = service.extract(URL);

    assertEquals("inhire", job.platform());
    assertEquals(URL, job.sourceUrl());
    assertEquals(64, job.contentHash().length());
    assertFalse(job.warnings().contains("sections_not_detected"));
  }

  @Test
  void serviceReportsNoContentWhenTheJobIsGone() {
    JobPageFetcher fetcher = url -> {
      throw new AssertionError("a página da InHire não deve ser baixada");
    };
    var service = new JobPostingExtractionService(fetcher, extractorReturning(null, null),
        new GupyJobExtractor(MAPPER), new JsonLdJobExtractor(), new GenericHtmlJobExtractor(), new PastedTextJobExtractor());

    UnprocessableRequestException error = assertThrows(UnprocessableRequestException.class, () -> service.extract(URL));
    assertEquals(ErrorCodes.Job.NO_CONTENT, error.getMessage());
  }
}
