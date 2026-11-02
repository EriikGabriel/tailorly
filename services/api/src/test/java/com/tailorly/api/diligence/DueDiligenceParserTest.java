package com.tailorly.api.diligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.diligence.dto.DueDiligenceReport.FactorResult;
import org.junit.jupiter.api.Test;

class DueDiligenceParserTest {
  private final DueDiligenceParser parser = new DueDiligenceParser(new ObjectMapper());
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
  private final Map<DiligenceFactor, List<DiligenceSource>> sources = Map.of(
      DiligenceFactor.LABOR_ISSUES, List.of(
          new DiligenceSource(1, "https://exemplo.com/a", "Acme é alvo de ação",
              "Funcionários relatam atrasos de salário na Acme Ltda.")),
      DiligenceFactor.CULTURE, List.of(
          new DiligenceSource(2, "https://exemplo.com/b", "Cultura", "Valores da Acme.")));

  private FactorResult factor(List<FactorResult> results, DiligenceFactor factor) {
    return results.stream().filter(result -> result.factor().equals(factor.id())).findFirst().orElseThrow();
  }

  @Test
  void alwaysReturnsAllElevenFactorsAndNoDataIsNotAProblem() {
    List<FactorResult> results = parser.parse("{\"factors\":[]}", sources, EnumSet.noneOf(DiligenceFactor.class), now);

    assertEquals(11, results.size());
    assertTrue(results.stream().allMatch(result -> "no_data".equals(result.status()) && result.findings().isEmpty()));
  }

  @Test
  void keepsFindingWithVerifiedExcerptAndForcesHighUncertaintyForReports() {
    String response = """
        {"factors":[{"factor":"labor_issues","summary":"Há relatos.","uncertaintyLevel":"low",
          "findings":[{"claim":"Segundo relatos, há atraso de salário","sourceId":1,
            "excerpt":"FUNCIONARIOS relatam atrasos de salario","sourceType":"review","polarity":"negative",
            "entityMatch":"confirmed"}]}]}
        """;
    FactorResult labor = factor(
        parser.parse(response, sources, EnumSet.noneOf(DiligenceFactor.class), now), DiligenceFactor.LABOR_ISSUES);

    assertEquals("evidence_found", labor.status());
    assertEquals("high", labor.uncertainty().level());
    assertTrue(labor.findings().get(0).unverifiedReport());
    assertEquals(now, labor.findings().get(0).retrievedAt());
  }

  @Test
  void discardsFindingsWithInventedExcerptOrForeignSource() {
    String response = """
        {"factors":[{"factor":"labor_issues","summary":"x","uncertaintyLevel":"low",
          "findings":[
           {"claim":"inventado","sourceId":1,"excerpt":"a empresa foi condenada","sourceType":"news",
            "polarity":"negative","entityMatch":"confirmed"},
           {"claim":"fonte de outro fator","sourceId":2,"excerpt":"Valores da Acme.","sourceType":"company",
            "polarity":"neutral","entityMatch":"confirmed"}]}]}
        """;
    FactorResult labor = factor(
        parser.parse(response, sources, EnumSet.noneOf(DiligenceFactor.class), now), DiligenceFactor.LABOR_ISSUES);

    assertEquals("no_data", labor.status());
  }

  @Test
  void marksFactorsWhoseSearchFailedAsSourceUnavailable() {
    List<FactorResult> results = parser.parse("{\"factors\":[]}", sources, EnumSet.of(DiligenceFactor.PARTNERS), now);

    assertEquals("source_unavailable", factor(results, DiligenceFactor.PARTNERS).status());
  }
}
