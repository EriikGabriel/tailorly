package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class JsonLdJobExtractorTest {
  private final JsonLdJobExtractor extractor = new JsonLdJobExtractor(new ObjectMapper());

  private JobPostingResponse extract(String jsonLd) {
    String html = "<html><head><script type=\"application/ld+json\">" + jsonLd + "</script></head><body></body></html>";
    return extractor.extract(Jsoup.parse(html), "https://jobs.test/1").orElseThrow();
  }

  @Test
  void readsSchemaOrgJobPosting() {
    JobPostingResponse job = extract("""
        {"@context":"https://schema.org","@type":"JobPosting","title":"Backend Engineer",
        "identifier":{"@type":"PropertyValue","value":"REQ-9"},
        "description":"&lt;p&gt;Sobre a vaga&lt;/p&gt;&lt;h3&gt;Requisitos&lt;/h3&gt;&lt;ul&gt;&lt;li&gt;Java&lt;/li&gt;&lt;/ul&gt;&lt;h3&gt;Benefícios&lt;/h3&gt;&lt;ul&gt;&lt;li&gt;VR&lt;/li&gt;&lt;/ul&gt;",
        "datePosted":"2026-10-01","validThrough":"2026-12-31T23:59:00Z",
        "employmentType":["FULL_TIME","CONTRACTOR"],"jobLocationType":"TELECOMMUTE",
        "hiringOrganization":{"@type":"Organization","name":"Acme","sameAs":["https://www.linkedin.com/company/acme","javascript:alert(1)"],
          "logo":{"@type":"ImageObject","url":"https://acme.test/logo.png"},"url":"https://acme.test"},
        "jobLocation":[{"@type":"Place","address":{"addressLocality":"Recife","addressRegion":"PE","addressCountry":{"name":"BR"}}}],
        "baseSalary":{"@type":"MonetaryAmount","currency":"BRL","value":{"@type":"QuantitativeValue","minValue":8000,"maxValue":"12000.50","unitText":"MONTH"}},
        "skills":"Java, Spring; Kafka"}
        """);

    assertEquals("json-ld", job.platform());
    assertEquals("REQ-9", job.externalId());
    assertEquals("Backend Engineer", job.title());
    assertEquals("remote", job.workModel());
    assertEquals("Tempo integral, Contrato (PJ)", job.employmentType());
    assertEquals("2026-10-01", job.publishedAt());
    assertEquals("2026-12-31T23:59:00Z", job.expiresAt());
    assertEquals(List.of("Java"), job.requirements());
    assertEquals(List.of("VR"), job.benefits());
    assertEquals(List.of("Java", "Spring", "Kafka"), job.skills());
    assertEquals("Recife", job.location().city());
    assertEquals("PE", job.location().state());
    assertEquals("BR", job.location().country());
    assertEquals(new BigDecimal("8000"), job.salary().min());
    assertEquals(new BigDecimal("12000.50"), job.salary().max());
    assertEquals("BRL", job.salary().currency());
    assertEquals("month", job.salary().period());
    assertEquals("Acme", job.company().name());
    assertEquals("https://acme.test/logo.png", job.company().logoUrl());
    assertEquals(List.of("linkedin"), List.copyOf(job.company().links().keySet()));
  }

  @Test
  void findsJobPostingInsideGraphAndSkipsBrokenScripts() {
    String html = "<script type=\"application/ld+json\">{broken</script>"
        + "<script type=\"application/ld+json\">{\"@graph\":[{\"@type\":\"WebSite\"},{\"@type\":[\"JobPosting\"],"
        + "\"title\":\"Dev\",\"description\":\"Texto\",\"hiringOrganization\":\"Solo Ltda\","
        + "\"experienceRequirements\":\"3 anos com Java\",\"educationRequirements\":{\"credentialCategory\":\"bachelor degree\"}}]}</script>";

    JobPostingResponse job = extractor.extract(Jsoup.parse(html), "https://jobs.test/2").orElseThrow();

    assertEquals("Dev", job.title());
    assertEquals("Solo Ltda", job.company().name());
    assertEquals(List.of("3 anos com Java", "bachelor degree"), job.requirements());
  }

  @Test
  void ignoresPagesWithoutJobPosting() {
    String html = "<script type=\"application/ld+json\">{\"@type\":\"Organization\",\"name\":\"X\"}</script>";

    assertTrue(extractor.extract(Jsoup.parse(html), "https://jobs.test/3").isEmpty());
    assertTrue(extractor.extract(Jsoup.parse("<p>sem dados</p>"), "https://jobs.test/3").isEmpty());
  }

  @Test
  void toleratesOddSalaryAndLocationShapes() {
    JobPostingResponse job = extract("""
        {"@type":"JobPosting","title":"X","description":"y","baseSalary":{"currency":"USD","value":5000},
        "jobLocation":{"address":"Lisboa, Portugal"}}
        """);

    assertEquals(new BigDecimal("5000"), job.salary().min());
    assertEquals(new BigDecimal("5000"), job.salary().max());
    assertEquals("Lisboa, Portugal", job.location().raw());
  }
}
