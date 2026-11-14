package com.tailorly.api.aipreferences;

import java.time.Duration;
import java.util.Map;

/**
 * Configuração de IA já resolvida para uma extração: preferência salva pelo owner com
 * fallback campo a campo para a configuração global do servidor
 * ({@code tailorly.interpretation.*}) onde o owner não configurou nada. Consumida por
 * {@code com.tailorly.api.extraction.interpreting}.
 *
 * <p>{@code claudeCli}/{@code codexCli} trazem só o que a conta sobrescreveu nos providers locais
 * {@code claude-cli}/{@code codex-cli}; nulos (ou campos nulos) usam os padrões do servidor.
 */
public record EffectiveAiSettings(
    String provider,
    String remoteEndpoint,
    String remoteModel,
    String remoteApiKey,
    String ollamaEndpoint,
    String ollamaModel,
    String ollamaApiKey,
    LocalCliOverrides claudeCli,
    LocalCliOverrides codexCli
) {
  public EffectiveAiSettings(String provider, String remoteEndpoint, String remoteModel, String remoteApiKey,
      String ollamaEndpoint, String ollamaModel, String ollamaApiKey) {
    this(provider, remoteEndpoint, remoteModel, remoteApiKey, ollamaEndpoint, ollamaModel, ollamaApiKey, null, null);
  }

  /** Escolhas da conta para um CLI local; campos nulos usam o servidor. */
  public record LocalCliOverrides(String model, Duration timeout, Map<String, String> efforts) {
  }
}
