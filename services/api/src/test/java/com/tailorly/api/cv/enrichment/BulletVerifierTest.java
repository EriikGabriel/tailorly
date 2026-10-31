package com.tailorly.api.cv.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.tailorly.api.cv.enrichment.BulletVerifier.ProposedBullet;
import org.junit.jupiter.api.Test;

class BulletVerifierTest {
  private static final Map<String, String> SOURCES = Map.of(
      "title", "Tailorly Plataforma de currículos",
      "evidence:1", "Reduziu o tempo de geração de 40 para 12 segundos usando cache em Redis.");

  private static int verdict(String text, String... anchors) {
    return BulletVerifier.verify(List.of(new ProposedBullet(text, List.of(anchors))), SOURCES, 4, 240).kept().size();
  }

  @Test
  void keepsBulletWhoseNumbersAndNamesAreInCitedSources() {
    assertEquals(1, verdict("Reduziu o tempo de geração de 40 para 12 segundos com cache em Redis.", "evidence:1"));
  }

  @Test
  void rejectsBulletWithoutAnchors() {
    assertEquals(0, verdict("Reduziu o tempo de geração para 12 segundos.", new String[0]));
  }

  @Test
  void rejectsBulletCitingUnknownSource() {
    assertEquals(0, verdict("Reduziu o tempo de geração para 12 segundos.", "evidence:999"));
  }

  @Test
  void rejectsInventedNumber() {
    assertEquals(0, verdict("Reduziu o tempo de geração para 5 segundos.", "evidence:1"));
  }

  @Test
  void rejectsInventedToolName() {
    assertEquals(0, verdict("Reduziu o tempo de geração usando Kubernetes.", "evidence:1"));
  }

  @Test
  void allowsCapitalizedWordAtSentenceStart() {
    assertEquals(1, verdict("Reduziu o tempo de geração com cache.", "evidence:1"));
  }

  @Test
  void numbersOnlyCountWhenCitedSourcesContainThem() {
    // 12 existe no texto da evidência, mas o bullet cita só o título, que não tem número.
    assertEquals(0, verdict("Chegou a 12 segundos de resposta.", "title"));
  }

  @Test
  void numberMustMatchAsWholeTokenNotAsSubstring() {
    // "12" existe na evidência citada, então a frase passa; "120" não existe e é rejeitada.
    assertEquals(1, verdict("Chegou a 12 segundos de resposta.", "evidence:1", "title"));
    assertEquals(0, verdict("Atende 120 usuários.", "evidence:1"));
  }

  @Test
  void dropsOverlongAndDuplicatedBullets() {
    String tooLong = "Reduziu " + "o tempo ".repeat(40) + "de geração.";
    assertEquals(0, verdict(tooLong, "evidence:1"));

    BulletVerifier.Verdict verdict = BulletVerifier.verify(List.of(
        new ProposedBullet("Reduziu o tempo de geração com cache.", List.of("evidence:1")),
        new ProposedBullet("reduziu o tempo de geração com cache.", List.of("evidence:1"))),
        SOURCES, 4, 240);
    assertEquals(1, verdict.kept().size());
    assertEquals(1, verdict.rejected());
  }

  @Test
  void capsTheNumberOfKeptBullets() {
    List<ProposedBullet> many = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) {
      many.add(new ProposedBullet("Reduziu o tempo de geração na etapa " + "alfa beta gama delta zeta eta".split(" ")[i] + ".",
          List.of("evidence:1")));
    }
    BulletVerifier.Verdict verdict = BulletVerifier.verify(many, SOURCES, 4, 240);
    assertEquals(4, verdict.kept().size());
    assertTrue(verdict.rejected() > 0);
  }
}
