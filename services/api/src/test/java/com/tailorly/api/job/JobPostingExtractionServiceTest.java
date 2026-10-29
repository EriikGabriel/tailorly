package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class JobPostingExtractionServiceTest {
  private static final String LONG_TEXT =
      "Estamos contratando uma pessoa para integrar o time de plataforma e cuidar da evolução dos serviços "
          + "internos, com foco em confiabilidade, observabilidade e experiência de quem desenvolve software. ";

  private static JobPostingExtractionService serviceFor(String html, String finalUrl) {
    JobPageFetcher fetcher = url -> new FetchedPage(Jsoup.parse(html, finalUrl), finalUrl);
    ObjectMapper mapper = new ObjectMapper();
    return new JobPostingExtractionService(fetcher, new GupyJobExtractor(mapper), new JsonLdJobExtractor(mapper),
        new GenericHtmlJobExtractor(), new PastedTextJobExtractor());
  }

  private static String gupyPage(String expiresAt) {
    return "<html><head><meta property=\"og:title\" content=\"Titulo OG\"></head><body><script id=\"__NEXT_DATA__\" "
        + "type=\"application/json\">{\"props\":{\"pageProps\":{\"job\":{\"id\":7,\"name\":\"Dev Gupy\","
        + "\"status\":\"published\",\"description\":\"<p>Texto</p>\",\"prerequisites\":\"Java.Spring.\","
        + "\"registerEndDate\":\"" + expiresAt + "\",\"jobSteps\":[],\"careerPage\":{\"name\":\"Acme\"}}}}}</script>"
        + "<main><p>" + LONG_TEXT + "</p></main></body></html>";
  }

  @Test
  void gupyDataWinsAndRecordsTraceabilityFields() {
    JobPostingResponse job = serviceFor(gupyPage("2999-01-01"), "https://acme.gupy.io/jobs/7")
        .extract("  https://acme.gupy.io/jobs/7?utm=x ");

    assertEquals("gupy", job.platform());
    assertEquals("Dev Gupy", job.title());
    assertEquals("https://acme.gupy.io/jobs/7?utm=x", job.sourceUrl());
    assertEquals("https://acme.gupy.io/jobs/7", job.finalUrl());
    assertNotNull(job.fetchedAt());
    assertTrue(job.fetchedAt().isBefore(Instant.now().plusSeconds(5)));
    assertEquals(64, job.contentHash().length());
    assertEquals(false, job.expired());
    assertFalse(job.warnings().contains("expired"));
    assertEquals(java.util.List.of("Java.", "Spring."), job.requirements());
  }

  @Test
  void sameContentProducesTheSameHash() {
    JobPostingResponse first = serviceFor(gupyPage("2999-01-01"), "https://acme.gupy.io/jobs/7").extract("https://a.test/1");
    JobPostingResponse second = serviceFor(gupyPage("2999-01-01"), "https://acme.gupy.io/jobs/7").extract("https://a.test/1");

    assertEquals(first.contentHash(), second.contentHash());
  }

  @Test
  void marksPastDeadlinesAsExpired() {
    JobPostingResponse job = serviceFor(gupyPage("2020-01-01"), "https://acme.gupy.io/jobs/7").extract("https://a.test/1");

    assertEquals(true, job.expired());
    assertTrue(job.warnings().contains("expired"));
  }

  @Test
  void genericFillsWhatStructuredDataLacks() {
    String html = "<html><head><script type=\"application/ld+json\">{\"@type\":\"JobPosting\",\"title\":\"Dev\","
        + "\"description\":\"Resumo curto\"}</script><link rel=\"canonical\" href=\"https://x.test/vaga\"></head>"
        + "<body><main><p>" + LONG_TEXT + "</p></main></body></html>";

    JobPostingResponse job = serviceFor(html, "https://x.test/vaga").extract("https://x.test/vaga");

    assertEquals("json-ld", job.platform());
    assertEquals("https://x.test/vaga", job.canonicalUrl());
    assertEquals("Resumo curto", job.descriptionText());
    assertFalse(job.warnings().contains("limited_content"));
    assertTrue(job.warnings().contains("sections_not_detected"));
  }

  @Test
  void expirationWithoutDateLeavesExpiredUnknown() {
    String html = "<script type=\"application/ld+json\">{\"@type\":\"JobPosting\",\"title\":\"Dev\",\"description\":\"d\"}</script>";

    assertNull(serviceFor(html, "https://x.test/").extract("https://x.test/").expired());
  }

  @Test
  void pagesThatOnlyHoldAShellAreNotPresentedAsJobs() {
    String shell = "<html><head><title>Entrar | Site</title></head><body><div id=\"root\"></div></body></html>";

    UnprocessableRequestException error = assertThrows(UnprocessableRequestException.class,
        () -> serviceFor(shell, "https://spa.test/").extract("https://spa.test/"));

    assertEquals(ErrorCodes.Job.NO_CONTENT, error.getMessage());
  }

  @Test
  void pastedTextIsClassifiedByItsOwnHeadings() {
    String text = "Engenheira de Dados\nVenha trabalhar com dados em escala.\nRequisitos\nSQL\nPython\nDiferenciais\nSpark\nBenefícios\nVR";

    JobPostingResponse job = serviceFor("", "https://x.test/").extractFromText(text, null, "Acme");

    assertEquals("text", job.platform());
    assertEquals("Engenheira de Dados", job.title());
    assertEquals("Acme", job.company().name());
    assertNull(job.sourceUrl());
    assertEquals(java.util.List.of("SQL", "Python"), job.requirements());
    assertEquals(java.util.List.of("Spark"), job.niceToHave());
    assertEquals(java.util.List.of("VR"), job.benefits());
    assertEquals(64, job.contentHash().length());
  }

  @Test
  void pastedTextKeepsAngleBracketsAndRejectsBlankText() {
    JobPostingExtractionService service = serviceFor("", "https://x.test/");

    JobPostingResponse job = service.extractFromText("Dev C++\nUsamos templates <T> e std::vector<int>.", "Dev", null);

    assertTrue(job.descriptionText().contains("std::vector<int>"));
    assertThrows(UnprocessableRequestException.class, () -> service.extractFromText("   \n ", null, null));
  }

  @Test
  void hashChangesWhenStructuredFieldsChangeButNotWithReadingMetadata() {
    JobPostingResponse base = JobPostingResponse.builder().title("Dev").descriptionText("Texto").expiresAt("2026-12-01").build();

    assertEquals(JobPostingExtractionService.versionHash(base),
        JobPostingExtractionService.versionHash(base.toBuilder().warnings(java.util.List.of("expired")).build()));
    assertTrue(!JobPostingExtractionService.versionHash(base)
        .equals(JobPostingExtractionService.versionHash(base.toBuilder().expiresAt("2026-12-02").build())));
  }

  @Test
  void parsesDatesAndDateTimes() {
    assertEquals(Instant.parse("2026-11-29T00:00:00Z"), JobPostingExtractionService.parseInstant("2026-11-28"));
    assertEquals(Instant.parse("2026-11-28T10:00:00Z"), JobPostingExtractionService.parseInstant("2026-11-28T10:00:00Z"));
    assertEquals(Instant.parse("2026-11-28T13:00:00Z"),
        JobPostingExtractionService.parseInstant("2026-11-28T10:00:00-03:00"));
    assertNull(JobPostingExtractionService.parseInstant("em breve"));
    assertNull(JobPostingExtractionService.parseInstant(" "));
  }
}
