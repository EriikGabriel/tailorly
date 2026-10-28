package com.tailorly.api.extraction.interpreting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingCurriculumInterpreterTest {
  @Mock AiPreferencesService preferences;
  @Mock AiCurriculumInterpreter ai;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID pageId = UUID.randomUUID();

  private RoutingCurriculumInterpreter router() {
    return new RoutingCurriculumInterpreter(preferences, new RuleBasedCurriculumInterpreter(), ai);
  }

  private EffectiveAiSettings settings(String provider) {
    return new EffectiveAiSettings(provider, "http://remote", "model", "key", "http://ollama", "model", "key");
  }

  @Test
  void rulesProviderOutputIsNormalizedButKeepsTheOriginalEvidence() {
    when(preferences.resolveEffectiveSettings(ownerId)).thenReturn(settings("rules"));
    var page = new ExtractedTextPage(pageId, 1, "pdf-text", "Contato\nTelefone: +55 11 98765-4321\nerik@example.com");

    List<FactProposal> result = router().interpret(ownerId, List.of(page));

    FactProposal phone = result.stream().filter(p -> p.fieldKey().equals("phone")).findFirst().orElseThrow();
    assertEquals("+55 (11) 98765-4321", phone.extractedValue());
    assertEquals("+55 11 98765-4321", phone.sourceExcerpt());
    assertEquals(phone.sourceExcerpt(),
        page.text().substring(phone.excerptStart(), phone.excerptEnd()));
  }

  @Test
  void aiProviderOutputIsNormalizedToo() {
    when(preferences.resolveEffectiveSettings(ownerId)).thenReturn(settings("remote"));
    var page = new ExtractedTextPage(pageId, 1, "pdf-text", "x");
    when(ai.interpret(any(), any())).thenReturn(List.of(
        new FactProposal("experience", "0:start", "jan/2022", pageId, "jan/2022", 0, 8),
        new FactProposal("experience", "0:end", "Atual", pageId, "Atual", 0, 5),
        new FactProposal("languages", "0:proficiency", "advanced", pageId, "advanced", 0, 8)));

    List<FactProposal> result = router().interpret(ownerId, List.of(page));

    assertEquals(List.of("0:start=2022-01", "0:status=Atual", "0:proficiency=Avançado"),
        result.stream().map(p -> p.fieldKey() + "=" + p.extractedValue()).toList());
  }
}
