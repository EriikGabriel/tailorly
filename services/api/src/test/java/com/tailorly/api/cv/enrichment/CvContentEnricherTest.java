package com.tailorly.api.cv.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import com.tailorly.api.cv.enrichment.BulletVerifier.ProposedBullet;
import com.tailorly.api.cv.model.CvDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CvContentEnricherTest {
  private final AiChatClient chat = mock(AiChatClient.class);
  private final AiPreferencesService preferences = mock(AiPreferencesService.class);
  private final CvContentEnricher enricher = new CvContentEnricher(new ObjectMapper(), chat, preferences);
  private final UUID owner = UUID.randomUUID();

  @BeforeEach
  void useRemoteProvider() {
    when(preferences.resolveEffectiveSettings(owner))
        .thenReturn(new EffectiveAiSettings("remote", "http://x", "m", "k", null, null, null));
  }

  private static CvDocument.Entry job(String id, String title, String company, String... details) {
    return new CvDocument.Entry(id, title, company, null, "2021 – Atual", List.of(details), null);
  }

  private static CvDocument document(String summary, CvDocument.Entry... jobs) {
    return new CvDocument("Ana", "Engenheira de Software", summary, List.of(),
        List.of(new CvDocument.Section("experience", "Experiência", false, List.of(jobs))));
  }

  private static List<ProposedBullet> bullets(String text, String... anchors) {
    return List.of(new ProposedBullet(text, List.of(anchors)));
  }

  @Test
  void onlyEntriesWithSomethingToRewriteBecomeTargets() {
    CvDocument document = document(null, job("experience:0", "Dev", "Acme", "Cuidou da API."),
        job("experience:1", "Estagiária", "Beta"));

    List<CvContentEnricher.Target> targets = CvContentEnricher.targets(document, List.of());

    assertEquals(List.of("experience:0"), targets.stream().map(CvContentEnricher.Target::id).toList());
  }

  @Test
  void summaryIsATargetOnlyWhenOneAlreadyExists() {
    assertTrue(CvContentEnricher.targets(document(null, job("experience:0", "Dev", "Acme", "x")), List.of())
        .stream().noneMatch(CvContentEnricher.Target::isSummary));
    assertTrue(CvContentEnricher.targets(document("Resumo.", job("experience:0", "Dev", "Acme", "x")), List.of())
        .stream().anyMatch(CvContentEnricher.Target::isSummary));
  }

  @Test
  void keepsNumbersDetectsLostMetrics() {
    assertTrue(CvContentEnricher.keepsNumbers(List.of("Reduziu custos em 40%."), List.of("Cortou custos em 40%.")));
    assertFalse(CvContentEnricher.keepsNumbers(List.of("Reduziu custos em 40%."), List.of("Reduziu custos.")));
  }

  @Test
  void rewritesAnchoredExperienceBulletsAndTheSummary() {
    CvDocument document = document("Engenheira de software focada em APIs.",
        job("experience:0", "Dev", "Acme", "Cuidou da API de pagamentos."));
    Map<String, List<ProposedBullet>> proposals = Map.of(
        "experience:0", bullets("Mantive a API de pagamentos da Acme.", "detail:0", "title"),
        "summary", bullets("Engenheira de Software focada em APIs, atuando como Dev na Acme.",
            "summary", "headline", "role:experience:0"));
    doReturn(proposals).when(chat).run(any(), anyString(), any());

    CvContentEnricher.Result result = enricher.enrich(owner, document, List.of(), null, "Dev");

    assertEquals("completed", result.enrichment().status());
    assertEquals(List.of("Mantive a API de pagamentos da Acme."),
        result.document().sections().getFirst().entries().getFirst().details());
    assertTrue(result.enrichment().summaryRewritten());
  }

  @Test
  void summaryCannotCiteExperienceDetails() {
    CvDocument document = document("Engenheira de software focada em APIs.",
        job("experience:0", "Dev", "Acme", "Cuidou da API de pagamentos."));
    doReturn(Map.of("summary", bullets("Engenheira focada em pagamentos.", "summary", "detail:0")))
        .when(chat).run(any(), anyString(), any());

    CvContentEnricher.Result result = enricher.enrich(owner, document, List.of(), null, "Dev");

    assertFalse(result.enrichment().summaryRewritten());
    assertEquals("Engenheira de software focada em APIs.", result.document().summary());
  }

  @Test
  void keepsOriginalTextWhenTheRewriteInventsANumberOrTool() {
    CvDocument document = document(null, job("experience:0", "Dev", "Acme", "Cuidou da API."));
    doReturn(Map.of("experience:0", bullets("Cuidou de 50 microsserviços em Kubernetes.", "detail:0")))
        .when(chat).run(any(), anyString(), any());

    CvContentEnricher.Result result = enricher.enrich(owner, document, List.of(), null, "Dev");

    assertEquals(List.of("Cuidou da API."), result.document().sections().getFirst().entries().getFirst().details());
    assertEquals(1, result.enrichment().bulletsRejected());
  }

  @Test
  void keepsOriginalTextWhenTheRewriteWouldLoseAMetric() {
    CvDocument document = document(null, job("experience:0", "Dev", "Acme", "Reduziu custos em 40%."));
    doReturn(Map.of("experience:0", bullets("Reduziu custos da operação.", "detail:0")))
        .when(chat).run(any(), anyString(), any());

    CvContentEnricher.Result result = enricher.enrich(owner, document, List.of(), null, "Dev");

    assertEquals(List.of("Reduziu custos em 40%."),
        result.document().sections().getFirst().entries().getFirst().details());
    assertEquals(1, result.enrichment().bulletsRejected());
  }

  @Test
  void reportsUnavailableWithoutAiAndFailedWhenTheAiFails() {
    CvDocument document = document(null, job("experience:0", "Dev", "Acme", "Cuidou da API."));

    when(preferences.resolveEffectiveSettings(owner))
        .thenReturn(new EffectiveAiSettings("rules", null, null, null, null, null, null));
    assertEquals("unavailable", enricher.enrich(owner, document, List.of(), null, "Dev").enrichment().status());

    when(preferences.resolveEffectiveSettings(owner))
        .thenReturn(new EffectiveAiSettings("remote", "http://x", "m", "k", null, null, null));
    doThrow(new IllegalStateException("down")).when(chat).run(any(), anyString(), any());
    CvContentEnricher.Result failed = enricher.enrich(owner, document, List.of(), null, "Dev");
    assertEquals("failed", failed.enrichment().status());
    assertEquals(document, failed.document());
  }
}
