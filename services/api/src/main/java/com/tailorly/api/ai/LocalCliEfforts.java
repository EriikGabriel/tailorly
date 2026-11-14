package com.tailorly.api.ai;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regras de esforço e limites comuns aos providers locais por CLI ({@code claude-cli} e
 * {@code codex-cli}): etapas conhecidas, níveis aceitos e como as escolhas da conta se combinam com
 * os padrões do servidor.
 */
public final class LocalCliEfforts {
  public static final Set<String> LEVELS = Set.of("low", "medium", "high", "xhigh", "max");
  /** Chave do esforço usado pelas etapas sem valor próprio. */
  public static final String DEFAULT_KEY = "default";
  /** Etapas que usam IA, na ordem do fluxo (nome da tarefa de {@link AiChatClient#run} com hífens). */
  public static final List<String> TASKS = List.of(
      "interpretation", "job-analysis", "due-diligence", "supplementary-evidence",
      "content-enrichment", "ats-review", "resume-layout-plan");
  public static final Duration MIN_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration MAX_TIMEOUT = Duration.ofMinutes(30);

  /** Padrões por etapa quando nada é configurado (ver comentários em application.yml). */
  static final Map<String, String> DEFAULT_EFFORTS = Map.of(
      "interpretation", "high",
      "content-enrichment", "high",
      "job-analysis", "medium",
      "supplementary-evidence", "medium",
      "due-diligence", "medium",
      "ats-review", "medium",
      "resume-layout-plan", "low");

  private LocalCliEfforts() {
  }

  public static String taskKey(String task) {
    return task == null ? "" : task.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
  }

  public static String normalize(String value) {
    if (value == null)
      return null;
    String level = value.trim().toLowerCase(Locale.ROOT);
    return LEVELS.contains(level) ? level : null;
  }

  /** Esforço do servidor para a etapa: configuração explícita, senão o padrão da etapa, senão o geral. */
  static String serverEffort(String task, Map<String, String> serverEfforts, String serverDefault) {
    String key = taskKey(task);
    String configured = normalize(serverEfforts.get(key));
    if (configured != null)
      return configured;
    String preset = DEFAULT_EFFORTS.get(key);
    return preset != null ? preset : serverDefault;
  }

  /**
   * Escolhas da conta por cima do servidor: valor da conta para a etapa, senão o do servidor para a
   * etapa; o {@code default} da conta só vale para etapas sem valor algum.
   */
  static String effort(String task, Map<String, String> serverEfforts, String serverDefault,
      Map<String, String> accountEfforts) {
    if (accountEfforts != null && !accountEfforts.isEmpty()) {
      String key = taskKey(task);
      String account = normalize(accountEfforts.get(key));
      if (account != null)
        return account;
      boolean serverHasTaskValue = normalize(serverEfforts.get(key)) != null || DEFAULT_EFFORTS.containsKey(key);
      String accountDefault = normalize(accountEfforts.get(DEFAULT_KEY));
      if (!serverHasTaskValue && accountDefault != null)
        return accountDefault;
    }
    return serverEffort(task, serverEfforts, serverDefault);
  }

  /** {@code default} seguido de cada etapa, com o valor do servidor. */
  static Map<String, String> serverEfforts(Map<String, String> serverEfforts, String serverDefault) {
    Map<String, String> resolved = new LinkedHashMap<>();
    resolved.put(DEFAULT_KEY, serverDefault);
    TASKS.forEach(task -> resolved.put(task, serverEffort(task, serverEfforts, serverDefault)));
    return resolved;
  }

  /** Nulo ou fora de {@link #MIN_TIMEOUT}..{@link #MAX_TIMEOUT} usa {@code fallback}. */
  static Duration timeoutOr(Duration requested, Duration fallback) {
    if (requested == null || requested.compareTo(MIN_TIMEOUT) < 0 || requested.compareTo(MAX_TIMEOUT) > 0)
      return fallback;
    return requested;
  }
}
