package com.tailorly.api.cv.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HeadlineVerifierTest {
  private static final Map<String, String> SOURCES = Map.of(
      "headline", "Engenheira de Software",
      "role:experience:0", "Desenvolvedora Acme 2021 – Atual Serviços em Java e Spring",
      "technologies", "Java, Spring Boot, React, TypeScript, PostgreSQL");

  private static String verify(String headline, String... anchors) {
    return HeadlineVerifier.verify(headline, List.of(anchors), SOURCES);
  }

  @Test
  void acceptsAJobTitleTheSourcesSupport() {
    assertEquals("Desenvolvedora Full Stack", verify("Desenvolvedora Full Stack", "technologies", "role:experience:0"));
    assertEquals("Software Engineer", verify("Software Engineer", "headline"));
    assertEquals("Desenvolvedor Back-end Java", verify("Desenvolvedor Back-end Java", "technologies"));
  }

  @Test
  void rejectsSeniorityThatTheSourcesDoNotState() {
    assertNull(verify("Desenvolvedora Full Stack Sênior", "technologies"));
    assertNull(verify("Tech Lead Back-end", "technologies"));
  }

  @Test
  void rejectsTechnologiesAndSpecialtiesWithoutSupport() {
    assertNull(verify("Desenvolvedor Rust", "technologies"));
    assertNull(verify("Desenvolvedor Mobile", "technologies"));
    assertNull(verify("Engenheiro DevOps", "technologies"));
  }

  @Test
  void rejectsMissingAnchorsDigitsAndLongTitles() {
    assertNull(verify("Desenvolvedor Full Stack"));
    assertNull(verify("Desenvolvedor Full Stack", "unknown"));
    assertNull(verify("Desenvolvedor 10x", "technologies"));
    assertNull(verify("Desenvolvedor Full Stack com Java, Spring, React, TypeScript e PostgreSQL", "technologies"));
  }

  @Test
  void frontEndNeedsAFrontEndTechnology() {
    assertEquals("Desenvolvedora Front-end", verify("Desenvolvedora Front-end", "technologies"));
    Map<String, String> backOnly = Map.of("technologies", "Java, Spring Boot, PostgreSQL");
    assertNull(HeadlineVerifier.verify("Desenvolvedor Front-end", List.of("technologies"), backOnly));
  }
}
