package com.tailorly.api.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider {@code claude-cli}: usa o Claude Code instalado e logado na própria máquina. Serve só para
 * uso local da pessoa dona da assinatura; nunca deve ser habilitado em deploy que atenda outras pessoas.
 * As regras de esforço por etapa estão em {@link LocalCliEfforts}.
 */
@ConfigurationProperties(prefix = "tailorly.interpretation.claude-cli")
public record ClaudeCliProperties(
    boolean enabled, String command, String model, Duration timeout, String effort, Map<String, String> efforts)
    implements LocalCliProperties {
  public static final Set<String> EFFORT_LEVELS = LocalCliEfforts.LEVELS;
  public static final String DEFAULT_KEY = LocalCliEfforts.DEFAULT_KEY;
  public static final List<String> TASKS = LocalCliEfforts.TASKS;
  public static final Duration MIN_TIMEOUT = LocalCliEfforts.MIN_TIMEOUT;
  public static final Duration MAX_TIMEOUT = LocalCliEfforts.MAX_TIMEOUT;

  public ClaudeCliProperties {
    command = command == null || command.isBlank() ? "claude" : command;
    model = model == null || model.isBlank() ? "sonnet" : model;
    timeout = timeout == null ? Duration.ofMinutes(3) : timeout;
    effort = LocalCliEfforts.normalize(effort) == null ? "medium" : LocalCliEfforts.normalize(effort);
    efforts = efforts == null ? Map.of() : Map.copyOf(efforts);
  }

  public static ClaudeCliProperties disabled() {
    return new ClaudeCliProperties(false, null, null, null, null, null);
  }

  public static String taskKey(String task) {
    return LocalCliEfforts.taskKey(task);
  }

  public static String normalizeEffort(String value) {
    return LocalCliEfforts.normalize(value);
  }
}
