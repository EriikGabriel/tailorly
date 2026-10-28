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
 * ({@code remote}, {@code remote-with-ollama-fallback}, {@code ollama-only}). O resultado de
 * qualquer caminho passa por {@link FactValueNormalizer}.
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

    List<FactProposal> proposals = "rules".equals(settings.provider())
        ? ruleBasedInterpreter.interpret(pages)
        : aiInterpreter.interpret(pages, settings);

    // Qualquer provedor entrega valores nos formatos que a revisão reconhece e edita.
    return FactValueNormalizer.normalize(proposals);
  }
}
