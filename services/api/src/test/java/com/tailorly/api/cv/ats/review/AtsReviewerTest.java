package com.tailorly.api.cv.ats.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.ats.parser.ResumeParser;
import com.tailorly.api.job.dto.JobAnalysis;
import org.junit.jupiter.api.Test;

class AtsReviewerTest {
  private final AtsReviewer reviewer = new AtsReviewer(new ObjectMapper(), null, null, new ResumeParser());

  private static final String RESUME = "Ana Souza\nExperiência profissional\nEngenheira — Acme 2021 – Atual\n"
      + "• Serviços em Java e Spring Boot com PostgreSQL\n";

  private static List<JobAnalysis.Requirement> requirements() {
    return List.of(
        new JobAnalysis.Requirement("Java", "required", "technology", null, List.of("Java"), "Java"),
        new JobAnalysis.Requirement("Kubernetes", "required", "technology", null, List.of("Kubernetes"), "k8s"),
        new JobAnalysis.Requirement("PostgreSQL", "preferred", "technology", null, List.of("PostgreSQL"), "pg"),
        new JobAnalysis.Requirement("Inglês", "preferred", "language", null, List.of(), "inglês"));
  }

  @Test
  void keepsOnlyEvidenceThatExistsInTheResumeText() {
    String response = """
        {"summary":"Boa aderência.","requirements":[
          {"id":1,"status":"met","evidence":"Serviços em Java e Spring Boot","note":"ok"},
          {"id":2,"status":"met","evidence":"Orquestração com Kubernetes em produção","note":"inventado"},
          {"id":3,"status":"partial","evidence":"com PostgreSQL","note":"sem anos"}],
         "strengths":["Java"],"suggestions":["Citar anos de PostgreSQL"]}
        """;

    AtsReview review = reviewer.parse(response, requirements(), RESUME);

    assertEquals("completed", review.status());
    assertEquals("met", review.requirements().get(0).status());
    // A citação que não existe no texto rebaixa o requisito para ausente.
    assertEquals("missing", review.requirements().get(1).status());
    assertNull(review.requirements().get(1).evidence());
    assertEquals("partial", review.requirements().get(2).status());
    // O que a IA não avaliou nunca vira atendido por omissão.
    assertEquals("missing", review.requirements().get(3).status());
  }

  @Test
  void alignmentWeightsRequiredTwiceAndPartialAsHalf() {
    AtsReview review = reviewer.parse("""
        {"requirements":[{"id":1,"status":"met","evidence":"Java"},
          {"id":3,"status":"partial","evidence":"PostgreSQL"}]}
        """, requirements(), RESUME);

    // required Java (2) met + preferred PostgreSQL (1) meio = 2.5 de 6 pontos.
    assertEquals(42, review.alignment());
  }

  @Test
  void unknownStatusAndForeignIdsAreIgnored() {
    AtsReview review = reviewer.parse("""
        {"requirements":[{"id":99,"status":"met","evidence":"Java"},{"id":1,"status":"great","evidence":"Java"}]}
        """, requirements(), RESUME);

    assertEquals("missing", review.requirements().get(0).status());
  }
}
