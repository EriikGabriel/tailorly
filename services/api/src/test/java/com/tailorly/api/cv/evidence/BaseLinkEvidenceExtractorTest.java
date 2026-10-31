package com.tailorly.api.cv.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BaseLinkEvidenceExtractorTest {
  private static final String PAGE = """
      Tailorly é um projeto de  código aberto.
      Reduziu o tempo de geração de 40 para 12 segundos.
      """;

  @Test
  void keepsOnlyClaimsWhoseExcerptAppearsInThePage() {
    List<BaseLinkEvidenceExtractor.Claim> proposed = List.of(
        new BaseLinkEvidenceExtractor.Claim("Projeto de código aberto", "Tailorly é um projeto de código aberto."),
        new BaseLinkEvidenceExtractor.Claim("Usa Kubernetes", "Roda em Kubernetes com autoscaling"));

    List<BaseLinkEvidenceExtractor.Claim> kept = BaseLinkEvidenceExtractor.verified(proposed, PAGE);

    assertEquals(1, kept.size());
    assertEquals("Projeto de código aberto", kept.getFirst().claim());
  }

  @Test
  void comparesExcerptsIgnoringWhitespaceDifferences() {
    List<BaseLinkEvidenceExtractor.Claim> proposed = List.of(
        new BaseLinkEvidenceExtractor.Claim("Reduziu o tempo", "Reduziu o tempo  de geração de 40 para 12 segundos."));

    assertEquals(1, BaseLinkEvidenceExtractor.verified(proposed, PAGE).size());
  }

  @Test
  void dropsEmptyAndDuplicatedClaims() {
    List<BaseLinkEvidenceExtractor.Claim> proposed = List.of(
        new BaseLinkEvidenceExtractor.Claim("", "Tailorly é um projeto de código aberto."),
        new BaseLinkEvidenceExtractor.Claim("Projeto aberto", ""),
        new BaseLinkEvidenceExtractor.Claim("Projeto aberto", "Tailorly é um projeto de código aberto."),
        new BaseLinkEvidenceExtractor.Claim("projeto aberto", "Tailorly é um projeto de código aberto."));

    assertEquals(1, BaseLinkEvidenceExtractor.verified(proposed, PAGE).size());
  }

  @Test
  void capsTheNumberOfClaims() {
    List<BaseLinkEvidenceExtractor.Claim> proposed = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      proposed.add(new BaseLinkEvidenceExtractor.Claim("Detalhe " + i, "Reduziu o tempo"));
    }

    List<BaseLinkEvidenceExtractor.Claim> kept = BaseLinkEvidenceExtractor.verified(proposed, PAGE);

    assertEquals(BaseLinkEvidenceExtractor.MAX_CLAIMS, kept.size());
    assertTrue(kept.stream().allMatch(claim -> claim.excerpt().equals("Reduziu o tempo")));
  }
}
