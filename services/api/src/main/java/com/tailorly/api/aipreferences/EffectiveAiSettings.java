package com.tailorly.api.aipreferences;

/**
 * Configuração de IA já resolvida para uma extração: preferência salva pelo owner com
 * fallback campo a campo para a configuração global do servidor
 * ({@code tailorly.interpretation.*}) onde o owner não configurou nada. Consumida por
 * {@code com.tailorly.api.extraction.interpreting}.
 */
public record EffectiveAiSettings(
    String provider,
    String remoteEndpoint,
    String remoteModel,
    String remoteApiKey,
    String ollamaEndpoint,
    String ollamaModel,
    String ollamaApiKey
) {
}
