package com.tailorly.api.job.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.job.dto.JobPostingResponse;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class GenericHtmlJobExtractorTest {
  private final GenericHtmlJobExtractor extractor = new GenericHtmlJobExtractor();

  private static final String LONG_INTRO =
      "Estamos contratando uma pessoa para integrar o time de plataforma e cuidar da evolução dos serviços "
          + "internos, com foco em confiabilidade, observabilidade e experiência de quem desenvolve. ";

  @Test
  void usesTheMainBlockAndIgnoresNavigation() {
    String html = "<html><head><title>Vaga | Site</title>"
        + "<meta property=\"og:title\" content=\"Engenheira de Plataforma\">"
        + "<meta property=\"og:site_name\" content=\"Acme\">"
        + "<link rel=\"canonical\" href=\"https://acme.test/vagas/1\"></head><body>"
        + "<nav>Início Vagas Contato</nav>"
        + "<main><p>" + LONG_INTRO + "</p><h2>Requisitos</h2><ul><li>Go</li><li>Kubernetes</li></ul>"
        + "<h2>Benefícios</h2><ul><li>Plano de saúde</li></ul></main>"
        + "<footer>Todos os direitos reservados</footer></body></html>";

    JobPostingResponse job = extractor.extract(Jsoup.parse(html, "https://acme.test/vagas/1"), "https://acme.test/vagas/1")
        .orElseThrow();

    assertEquals("generic", job.platform());
    assertEquals("Engenheira de Plataforma", job.title());
    assertEquals("Acme", job.company().name());
    assertEquals("https://acme.test/vagas/1", job.canonicalUrl());
    assertEquals(List.of("Go", "Kubernetes"), job.requirements());
    assertEquals(List.of("Plano de saúde"), job.benefits());
    assertFalse(job.descriptionText().contains("Todos os direitos reservados"));
    assertFalse(job.descriptionText().contains("Início Vagas"));
    assertTrue(job.warnings().isEmpty());
  }

  @Test
  void flagsPagesWithLittleTextAsLimitedContent() {
    JobPostingResponse job = extractor.extract(
        Jsoup.parse("<html><head><title>Entrar</title></head><body><div id=\"root\"></div></body></html>"),
        "https://spa.test/").orElseThrow();

    assertTrue(job.warnings().contains("limited_content"));
  }

  @Test
  void doesNotMutateTheSharedDocument() {
    var document = Jsoup.parse("<html><body><nav>menu</nav><main>" + LONG_INTRO + "</main></body></html>");

    extractor.extract(document, "https://x.test/");

    assertEquals(1, document.select("nav").size());
  }

  @Test
  void returnsNothingForAnEmptyPage() {
    assertTrue(extractor.extract(Jsoup.parse(""), "https://x.test/").isEmpty());
  }
}
