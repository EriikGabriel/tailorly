package com.tailorly.api.cv.ats.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import tools.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.ats.parser.ResumeParser;
import com.tailorly.api.job.dto.JobAnalysis;
import org.junit.jupiter.api.Test;

class AtsReviewerTest {
  private final AtsReviewer reviewer = new AtsReviewer(new ObjectMapper(), null, null, new ResumeParser());

  private static final List<String> LINES = List.of(
      "Ana Souza",
      "Experiência profissional",
      "Engenheira — Acme 2021 – Atual",
      "• Serviços em Java e Spring Boot",
      "Tecnologias",
      "Frontend: React, TypeScript.",
      "Banco de dados: Postgres, MySQL.");

  private static JobAnalysis.Requirement requirement(String text, String importance, String category,
      Integer minYears, String... technologies) {
    return new JobAnalysis.Requirement(text, importance, category, minYears, List.of(technologies), text);
  }

  private static List<JobAnalysis.Requirement> requirements() {
    return List.of(
        requirement("Java", "required", "technology", null, "Java"),
        requirement("Kubernetes", "required", "technology", null, "Kubernetes"),
        requirement("PostgreSQL", "preferred", "technology", null, "PostgreSQL"),
        requirement("Inglês", "preferred", "language", null));
  }

  @Test
  void evidenceIsTheRealTextOfTheCitedLines() {
    String response = """
        {"summary":"Boa aderência.","requirements":[
          {"id":1,"status":"met","lines":[4],"note":"ok"},
          {"id":2,"status":"met","lines":[99],"note":"linha inexistente"}],
         "strengths":["Java"],"suggestions":["Citar anos"]}
        """;

    AtsReview review = reviewer.parse(response, requirements(), LINES);

    assertEquals("completed", review.status());
    assertEquals("met", review.requirements().get(0).status());
    assertEquals("• Serviços em Java e Spring Boot", review.requirements().get(0).evidence());
    // Um número de linha inexistente não sustenta nada: o requisito fica ausente.
    assertEquals("missing", review.requirements().get(1).status());
    assertNull(review.requirements().get(1).evidence());
  }

  @Test
  void aTechnologyThatAppearsInAnotherSpellingIsNeverMissing() {
    // A IA disse "ausente" para PostgreSQL, mas o currículo lista "Postgres".
    AtsReview review = reviewer.parse("""
        {"requirements":[{"id":3,"status":"missing","lines":[],"note":"não achei"}]}
        """, requirements(), LINES);

    var postgres = review.requirements().get(2);
    assertEquals("met", postgres.status());
    assertEquals("Banco de dados: Postgres, MySQL.", postgres.evidence());
  }

  @Test
  void aTechnologyRequirementWithYearsOnlyBecomesPartial() {
    var withYears = requirement("React com 3 anos", "required", "technology", 3, "React.js");

    var result = AtsReviewer.floor(withYears,
        new AtsReview.RequirementCheck("React com 3 anos", "required", "missing", null, "x"), LINES);

    assertEquals("partial", result.status());
    assertEquals("Frontend: React, TypeScript.", result.evidence());
  }

  @Test
  void whatTheTextDoesNotMentionStaysMissingAndUnevaluatedIsNeverMet() {
    AtsReview review = reviewer.parse("{\"requirements\":[]}", requirements(), LINES);

    assertEquals("met", review.requirements().get(0).status(), "Java aparece no texto");
    assertEquals("missing", review.requirements().get(1).status(), "Kubernetes não aparece");
    assertEquals("missing", review.requirements().get(3).status());
    assertTrue(review.requirements().get(3).note().contains("Sem avaliação"));
  }

  @Test
  void alignmentWeightsRequiredTwiceAndPartialAsHalf() {
    AtsReview review = reviewer.parse("""
        {"requirements":[{"id":1,"status":"met","lines":[4]},{"id":3,"status":"partial","lines":[7]}]}
        """, requirements(), LINES);

    // Java (obrigatório, peso 2) atendido + PostgreSQL (desejável, peso 1) parcial = 2,5 de 6 pontos.
    // Kubernetes e Inglês ficam ausentes: não aparecem no texto.
    assertEquals(42, review.alignment());
  }
}
