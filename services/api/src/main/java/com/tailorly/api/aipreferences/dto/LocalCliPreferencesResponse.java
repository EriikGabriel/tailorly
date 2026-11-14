package com.tailorly.api.aipreferences.dto;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tailorly.api.ai.LocalCliEfforts;
import com.tailorly.api.ai.LocalCliProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um provider local por CLI ({@code claude-cli} ou {@code codex-cli}) nas preferências da conta.
 * {@code available} diz se o servidor o habilitou (uso local); {@code model}, {@code timeoutSeconds} e
 * {@code efforts} são os valores efetivos (escolha da conta ou padrão do servidor) e {@code server*} os
 * padrões do servidor, para a interface mostrar o que foi personalizado. Modelo vazio = padrão do CLI.
 */
public record LocalCliPreferencesResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String model,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int timeoutSeconds,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, String> efforts,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String serverModel,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int serverTimeoutSeconds,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, String> serverEfforts
) {
  static LocalCliPreferencesResponse of(LocalCliProperties server, String accountModel, Integer accountTimeoutSeconds,
      Map<String, String> accountEfforts) {
    Map<String, String> serverEfforts = server.serverEfforts();
    Map<String, String> efforts = new LinkedHashMap<>(serverEfforts);
    if (accountEfforts != null)
      accountEfforts.forEach((key, value) -> {
        if (efforts.containsKey(key) && LocalCliEfforts.normalize(value) != null)
          efforts.put(key, LocalCliEfforts.normalize(value));
      });
    int serverTimeout = seconds(server.timeout());
    return new LocalCliPreferencesResponse(
        server.enabled(),
        accountModel != null && !accountModel.isBlank() ? accountModel : server.model(),
        accountTimeoutSeconds != null ? accountTimeoutSeconds : serverTimeout,
        efforts,
        server.model(),
        serverTimeout,
        serverEfforts);
  }

  static LocalCliPreferencesResponse serverDefaults(LocalCliProperties server) {
    return of(server, null, null, null);
  }

  private static int seconds(Duration duration) {
    return (int) duration.toSeconds();
  }
}
