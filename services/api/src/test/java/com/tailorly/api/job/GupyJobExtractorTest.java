package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class GupyJobExtractorTest {
  private final GupyJobExtractor extractor = new GupyJobExtractor(new ObjectMapper());

  // Estrutura observada em páginas reais da Gupy (props.pageProps.job), com texto sintético. Os itens
  // de requisitos chegam colados e com autolinks falsos, como na plataforma.
  private static final String NEXT_DATA = """
      {"props":{"pageProps":{"canonicalUrl":"https://acme.gupy.io/jobs/123?jobBoardSource=gupy_public_page",
      "job":{"id":123,"name":"Desenvolvedor Full Stack - Pleno","status":"published",
      "description":"<p>Venha construir produtos.</p><p><br></p><p>Venha construir produtos.</p>",
      "responsibilities":"Desenvolver APIs em <a href=http://Node.js/TypeScript>Node.js/TypeScript</a> e Java.Construir interfaces em React.Garantir entregas contínuas.",
      "prerequisites":"Experiência com Java e Spring.Conhecimento em <a href=http://Kubernetes.Familiaridade>Kubernetes.Familiaridade</a> com CI/CD.Desejável: Kafka, RabbitMQ.Diferenciais: Experiência em fintech.Graduação em Ciência da Computação.",
      "relevantExperiences":"<p>Benefícios:</p><ul><li>Vale Refeição</li><li>Plano de saúde</li></ul><p><br></p><h2>SOBRE A ACME</h2><p><strong>Somos a Acme.</strong></p>",
      "addressCity":"São Paulo","addressState":"São Paulo","addressStateShortName":"SP","addressCountry":"Brasil","addressLine":"Av. Exemplo, 1",
      "workplaceType":"hybrid","jobType":"vacancy_type_effective","publishedAt":"2026-10-28T18:30:29.525Z",
      "registerEndDate":"2026-11-28","expiresAt":"2026-11-28","skills":null,
      "jobSteps":[{"name":"Contratação","order":100},{"name":"Cadastro","order":0},{"name":"Entrevista técnica","order":3}],
      "company":{"subdomain":"acme"},
      "careerPage":{"name":"Acme | o futuro passa por aqui ","subdomain":"acme","urlLogo":"https://cdn.example.com/logo.png",
      "about":"<p><strong>Nós somos a Acme</strong></p><p>Conectamos o Brasil.</p>",
      "socialLinks":{"urlSite":"https://acme.com.br/","urlLinkedin":"https://www.linkedin.com/company/acme/","urlGlassdoor":"https://www.glassdoor.com.br/acme","urlInstagram":""}}}}}}
      """;

  private static String page(String nextData) {
    return "<html><head><title>Página da Vaga</title></head><body><script id=\"__NEXT_DATA__\" type=\"application/json\">"
        + nextData + "</script></body></html>";
  }

  private JobPostingResponse extract() {
    Optional<JobPostingResponse> result = extractor.extract(Jsoup.parse(page(NEXT_DATA)), "https://acme.gupy.io/jobs/123");
    assertTrue(result.isPresent());
    return result.get();
  }

  @Test
  void readsStructuredJobFields() {
    JobPostingResponse job = extract();

    assertEquals("gupy", job.platform());
    assertEquals("123", job.externalId());
    assertEquals("Desenvolvedor Full Stack - Pleno", job.title());
    assertEquals("published", job.status());
    assertEquals("hybrid", job.workModel());
    assertEquals("Efetivo", job.employmentType());
    assertEquals("2026-10-28T18:30:29.525Z", job.publishedAt());
    assertEquals("2026-11-28", job.expiresAt());
    assertEquals("https://acme.gupy.io/jobs/123?jobBoardSource=gupy_public_page", job.canonicalUrl());
    assertEquals("São Paulo", job.location().city());
    assertEquals("SP", job.location().state());
    assertEquals("Brasil", job.location().country());
  }

  @Test
  void hiringStepsFollowTheDeclaredOrder() {
    assertEquals(java.util.List.of("Cadastro", "Entrevista técnica", "Contratação"), extract().hiringProcess());
  }

  @Test
  void companyKeepsDueDiligenceLinksAndDropsTheSlogan() {
    JobPostingResponse.Company company = extract().company();

    assertEquals("Acme", company.name());
    assertEquals("https://acme.com.br/", company.website());
    assertEquals("https://acme.gupy.io", company.careerPageUrl());
    assertEquals("https://cdn.example.com/logo.png", company.logoUrl());
    assertEquals("https://www.linkedin.com/company/acme/", company.links().get("linkedin"));
    assertEquals("https://www.glassdoor.com.br/acme", company.links().get("glassdoor"));
    assertFalse(company.links().containsKey("instagram"), "links vazios não entram");
    assertTrue(company.description().contains("Conectamos o Brasil."));
  }

  @Test
  void splitsGluedListItemsAndStripsFalseAutolinks() {
    JobPostingResponse job = extract();

    assertEquals(java.util.List.of(
        "Desenvolver APIs em Node.js/TypeScript e Java.",
        "Construir interfaces em React.",
        "Garantir entregas contínuas."), job.responsibilities());
    assertTrue(job.requirements().contains("Experiência com Java e Spring."));
    assertTrue(job.requirements().contains("Conhecimento em Kubernetes."));
    assertTrue(job.requirements().contains("Familiaridade com CI/CD."), job.requirements().toString());
  }

  @Test
  void inlineLabelsClassifyOnlyTheirOwnItem() {
    JobPostingResponse job = extract();

    assertEquals(java.util.List.of("Kafka, RabbitMQ.", "Experiência em fintech."), job.niceToHave());
    // O requisito que vem depois de "Diferenciais:" continua sendo requisito.
    assertTrue(job.requirements().contains("Graduação em Ciência da Computação."));
  }

  @Test
  void benefitsEndAtTheNextRealHeading() {
    JobPostingResponse job = extract();

    assertEquals(java.util.List.of("Vale Refeição", "Plano de saúde"), job.benefits());
    assertFalse(job.benefits().stream().anyMatch(item -> item.contains("Somos a Acme")));
  }

  @Test
  void fullTextKeepsEveryBlockAndRepeatedLinesAreNotDuplicatedInSections() {
    JobPostingResponse job = extract();

    assertTrue(job.descriptionText().contains("Descrição\nVenha construir produtos."));
    assertTrue(job.descriptionText().contains("Requisitos e qualificações\n"));
    assertNotNull(job.descriptionText());
  }

  @Test
  void ignoresPagesWithoutGupyJobData() {
    assertTrue(extractor.extract(Jsoup.parse("<html><body>Olá</body></html>"), "https://x.test").isEmpty());
    assertTrue(extractor.extract(Jsoup.parse(page("{not json")), "https://x.test").isEmpty());
    assertTrue(extractor.extract(Jsoup.parse(page("{\"props\":{\"pageProps\":{\"job\":{\"name\":\"X\"}}}}")),
        "https://x.test").isEmpty(), "outro app Next.js com um campo 'job' qualquer não é Gupy");
  }

  @Test
  void warnsWhenTheJobIsNotPublished() {
    String closed = NEXT_DATA.replace("\"status\":\"published\"", "\"status\":\"closed\"");
    JobPostingResponse job = extractor.extract(Jsoup.parse(page(closed)), "https://acme.gupy.io/jobs/123").orElseThrow();

    assertTrue(job.warnings().contains("job_not_published"));
  }
}
