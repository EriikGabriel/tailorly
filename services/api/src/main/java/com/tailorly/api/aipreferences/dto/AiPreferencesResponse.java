package com.tailorly.api.aipreferences.dto;

import java.time.Instant;
import java.util.List;

import com.tailorly.api.ai.ClaudeCliProperties;
import com.tailorly.api.ai.CodexCliProperties;
import com.tailorly.api.ai.LocalCliEfforts;
import com.tailorly.api.aipreferences.AiPreferences;
import com.tailorly.api.extraction.interpreting.OllamaInterpretationProperties;
import com.tailorly.api.extraction.interpreting.RemoteInterpretationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * {@code accountOverride=false} significa que a conta nunca salvou preferência própria e está
 * usando a configuração global do servidor ({@code tailorly.interpretation.*}); chaves de API
 * nunca são devolvidas em texto puro, só se estão configuradas.
 *
 * <p>{@code claudeCli}/{@code codexCli}: providers locais por CLI (ver {@link LocalCliPreferencesResponse});
 * {@code localCliTasks} lista as etapas com IA, na ordem do fluxo, que aceitam esforço próprio.
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
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalCliPreferencesResponse claudeCli,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalCliPreferencesResponse codexCli,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> localCliTasks,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant updatedAt
) {
  public static AiPreferencesResponse from(AiPreferences preferences, ClaudeCliProperties claudeCli,
      CodexCliProperties codexCli) {
    return AiPreferencesResponse.builder()
        .claudeCli(LocalCliPreferencesResponse.of(claudeCli, preferences.getClaudeCliModel(),
            preferences.getClaudeCliTimeoutSeconds(), preferences.getClaudeCliEfforts()))
        .codexCli(LocalCliPreferencesResponse.of(codexCli, preferences.getCodexCliModel(),
            preferences.getCodexCliTimeoutSeconds(), preferences.getCodexCliEfforts()))
        .localCliTasks(LocalCliEfforts.TASKS)
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
      RemoteInterpretationProperties remote, OllamaInterpretationProperties ollama, ClaudeCliProperties claudeCli,
      CodexCliProperties codexCli) {
    return AiPreferencesResponse.builder()
        .claudeCli(LocalCliPreferencesResponse.serverDefaults(claudeCli))
        .codexCli(LocalCliPreferencesResponse.serverDefaults(codexCli))
        .localCliTasks(LocalCliEfforts.TASKS)
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
