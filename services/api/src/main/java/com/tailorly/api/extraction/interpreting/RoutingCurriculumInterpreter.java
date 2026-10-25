package com.tailorly.api.extraction.interpreting;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Único {@link CurriculumInterpreter} registrado: resolve a preferência de IA do owner (com
 * fallback para a configuração global do servidor, veja {@link AiPreferencesService}) e
 * encaminha para regras determinísticas ({@code rules}) ou para {@link AiCurriculumInterpreter}
 * ({@code remote}, {@code remote-with-ollama-fallback}, {@code ollama-only}).
 */
@Component
@RequiredArgsConstructor
public class RoutingCurriculumInterpreter implements CurriculumInterpreter {
  private final AiPreferencesService aiPreferencesService;
  private final RuleBasedCurriculumInterpreter ruleBasedInterpreter;
  private final AiCurriculumInterpreter aiInterpreter;

  @Override
  public List<FactProposal> interpret(UUID ownerId, List<ExtractedTextPage> pages) {
    EffectiveAiSettings settings = aiPreferencesService.resolveEffectiveSettings(ownerId);

    if ("rules".equals(settings.provider()))
      return ruleBasedInterpreter.interpret(pages);

    return aiInterpreter.interpret(pages, settings);
  }
}
