package com.tailorly.api.ai;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider {@code codex-cli}: usa o Codex CLI instalado e logado (conta ChatGPT) na própria máquina.
 * Serve só para uso local da pessoa dona da conta; nunca deve ser habilitado em deploy que atenda
 * outras pessoas. Modelo vazio usa o padrão do próprio Codex. As regras de esforço por etapa estão em
 * {@link LocalCliEfforts}.
 */
@ConfigurationProperties(prefix = "tailorly.interpretation.codex-cli")
public record CodexCliProperties(
    boolean enabled, String command, String model, Duration timeout, String effort, Map<String, String> efforts)
    implements LocalCliProperties {
  public CodexCliProperties {
    command = command == null || command.isBlank() ? "codex" : command;
    model = model == null ? "" : model.trim();
    timeout = timeout == null ? Duration.ofMinutes(3) : timeout;
    effort = LocalCliEfforts.normalize(effort) == null ? "medium" : LocalCliEfforts.normalize(effort);
    efforts = efforts == null ? Map.of() : Map.copyOf(efforts);
  }

  public static CodexCliProperties disabled() {
    return new CodexCliProperties(false, null, null, null, null, null);
  }
}
