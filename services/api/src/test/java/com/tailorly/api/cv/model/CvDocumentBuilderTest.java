package com.tailorly.api.cv.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.cv.model.CvDocument.Section;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CvDocumentBuilderTest {
  private static FactValue id(String key, String value) {
    return new FactValue("identity", 0, key, value);
  }

  private static FactValue item(String category, int order, String key, String value) {
    return new FactValue(category, order, key, value);
  }

  private static JobTerms terms(String... skills) {
    return JobTerms.of(JobPostingResponse.builder().skills(List.of(skills)).build(), null);
  }

  private static Section section(CvDocument document, String key) {
    return document.sections().stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
  }

  @Test
  @DisplayName("monta identidade, contatos com links e período a partir dos fatos aprovados")
  void buildsIdentityAndExperience() {
    var result = CvDocumentBuilder.build(List.of(
        id("name", "Ana Souza"), id("email", "ana@example.com"), id("linkedin", "linkedin.com/in/ana"),
        id("location", "São Paulo, SP"), id("phone", "(11) 98765-4321"),
        item("experience", 1, "0:company", "Acme"), item("experience", 1, "0:role", "Engenheira"),
        item("experience", 1, "0:start", "2021-05"), item("experience", 1, "0:status", "Atual"),
        item("experience", 1, "0:description", "Linha 1\nLinha 2")), terms());

    CvDocument document = result.document();
    assertEquals("Ana Souza", document.name());
    assertEquals(List.of("São Paulo, SP", "ana@example.com", "(11) 98765-4321", "linkedin.com/in/ana"),
        document.contacts().stream().map(CvDocument.Contact::text).toList());
    assertEquals("mailto:ana@example.com", document.contacts().get(1).url());
    assertNull(document.contacts().get(2).url());
    assertEquals("https://linkedin.com/in/ana", document.contacts().get(3).url());

    CvDocument.Entry entry = section(document, "experience").entries().getFirst();
    assertEquals("Engenheira", entry.title());
    assertEquals("Acme", entry.subtitle());
    assertEquals("05/2021 – Atual", entry.period());
    assertEquals(List.of("Linha 1\nLinha 2"), entry.details());
  }

  @Test
  @DisplayName("itens saem na ordem do currículo e seções sem fatos não existem")
  void keepsDocumentOrderAndOmitsEmptySections() {
    var result = CvDocumentBuilder.build(List.of(
        item("experience", 0, "1:company", "Segunda"), item("experience", 0, "1:role", "Dev"),
        item("experience", 0, "0:company", "Primeira"), item("experience", 0, "0:role", "Dev"),
        item("experience", 0, "10:company", "Décima"), item("experience", 0, "10:role", "Dev")), terms());

    assertEquals(List.of("experience"), result.document().sections().stream().map(Section::key).toList());
    assertEquals(List.of("Primeira", "Segunda", "Décima"),
        section(result.document(), "experience").entries().stream().map(CvDocument.Entry::subtitle).toList());
  }

  @Test
  @DisplayName("a vaga só reordena tecnologias e projetos que a pessoa já tem")
  void jobOnlyReordersExistingItems() {
    List<FactValue> facts = List.of(
        item("technologies", 0, "0:name", "Python"), item("technologies", 0, "1:name", "Java"),
        item("technologies", 0, "2:name", "C#"),
        item("projects", 1, "0:title", "Site pessoal"), item("projects", 1, "0:statement", "Blog em HTML"),
        item("projects", 1, "1:title", "API"), item("projects", 1, "1:statement", "Serviço em Java e Kubernetes"));

    var result = CvDocumentBuilder.build(facts, terms("java", "C#", "Terraform"));

    assertEquals(List.of("Java", "C#", "Python"),
        section(result.document(), "technologies").entries().stream().map(CvDocument.Entry::title).toList());
    assertEquals(List.of("API", "Site pessoal"),
        section(result.document(), "projects").entries().stream().map(CvDocument.Entry::title).toList());
    // Terraform foi pedido pela vaga, mas não existe nos fatos aprovados: não aparece nem como "encontrado".
    assertEquals(List.of("java", "C#"), result.matchedTerms());
    assertFalse(result.document().toString().contains("Terraform"));

    var withoutJob = CvDocumentBuilder.build(facts, terms());
    assertEquals(List.of("Python", "Java", "C#"),
        section(withoutJob.document(), "technologies").entries().stream().map(CvDocument.Entry::title).toList());
  }

  @Test
  @DisplayName("termos curtos só casam como palavra inteira")
  void termsMatchWholeWords() {
    assertTrue(terms("Go").matchedIn("Experiência com Go e Docker").contains("Go"));
    assertTrue(terms("Go").matchedIn("Mongo, Django, Google").isEmpty());
    assertTrue(terms("C++").matches("C++ e C"));
  }

  @Test
  @DisplayName("junta termos da análise e das competências sem repetir")
  void collectsTermsFromAnalysisAndSkills() {
    JobAnalysis analysis = JobAnalysis.builder()
        .technologies(List.of("Java", "Spring"))
        .requirements(List.of(new JobAnalysis.Requirement("x", "required", "technology", null,
            List.of("java", "Docker"), "x")))
        .build();
    JobTerms terms = JobTerms.of(JobPostingResponse.builder().skills(List.of("SPRING", "Kafka")).build(), analysis);

    assertEquals(List.of("Java", "Spring", "Docker", "Kafka"), terms.terms());
  }

  @Test
  @DisplayName("certificação, formação e idiomas usam só os campos aprovados")
  void formatsOtherSections() {
    var result = CvDocumentBuilder.build(List.of(
        item("degrees", 0, "0:degree", "Ciência da Computação"), item("degrees", 0, "0:institution", "USP"),
        item("degrees", 0, "0:start", "2020"), item("degrees", 0, "0:status", "Em andamento"),
        item("certifications", 1, "0:name", "AWS SAA"), item("certifications", 1, "0:issued", "2023-01"),
        item("certifications", 1, "0:credential", "ABC123"),
        item("languages", 2, "0:language", "Inglês"), item("languages", 2, "0:proficiency", "Avançado")), terms());

    assertEquals("2020 – Em andamento", section(result.document(), "degrees").entries().getFirst().period());
    CvDocument.Entry certification = section(result.document(), "certifications").entries().getFirst();
    assertEquals("01/2023", certification.period());
    assertEquals(List.of("Credencial: ABC123"), certification.details());
    assertTrue(section(result.document(), "languages").compact());
    assertEquals("Avançado", section(result.document(), "languages").entries().getFirst().subtitle());
  }
}
