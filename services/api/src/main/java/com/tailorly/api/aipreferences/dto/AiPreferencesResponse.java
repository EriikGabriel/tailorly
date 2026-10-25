package com.tailorly.api.aipreferences.dto;

import java.time.Instant;

import com.tailorly.api.aipreferences.AiPreferences;
import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * {@code accountOverride=false} significa que a conta nunca salvou preferência própria e está
 * usando a configuração global do servidor ({@code tailorly.interpretation.*}); chaves de API
 * nunca são devolvidas em texto puro, só se estão configuradas.
 */
@Builder
public record AiPreferencesResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String provider,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean accountOverride,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String remoteEndpoint,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String remoteModel,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean remoteApiKeyConfigured,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String ollamaEndpoint,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String ollamaModel,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean ollamaApiKeyConfigured,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant updatedAt
) {
  public static AiPreferencesResponse from(AiPreferences preferences) {
    return AiPreferencesResponse.builder()
        .provider(preferences.getProvider())
        .accountOverride(true)
        .remoteEndpoint(preferences.getRemoteEndpoint())
        .remoteModel(preferences.getRemoteModel())
        .remoteApiKeyConfigured(hasText(preferences.getRemoteApiKeyEncrypted()))
        .ollamaEndpoint(preferences.getOllamaEndpoint())
        .ollamaModel(preferences.getOllamaModel())
        .ollamaApiKeyConfigured(hasText(preferences.getOllamaApiKeyEncrypted()))
        .updatedAt(preferences.getUpdatedAt())
        .build();
  }

  public static AiPreferencesResponse serverDefaults(String defaultProvider,
      RemoteInterpretationProperties remote, OllamaInterpretationProperties ollama) {
    return AiPreferencesResponse.builder()
        .provider(defaultProvider)
        .accountOverride(false)
        .remoteEndpoint(remote.endpoint())
        .remoteModel(remote.model())
        .remoteApiKeyConfigured(hasText(remote.apiKey()))
        .ollamaEndpoint(ollama.endpoint())
        .ollamaModel(ollama.model())
        .ollamaApiKeyConfigured(hasText(ollama.apiKey()))
        .updatedAt(null)
        .build();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
