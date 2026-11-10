package com.tailorly.api.ai;

import java.time.Duration;
import java.util.Map;

/**
 * Configuração de servidor de um provider local por CLI. {@code enabled} e {@code command} são
 * exclusivos do servidor; modelo, timeout e esforços são padrões que a conta pode sobrescrever.
 */
public interface LocalCliProperties {
  boolean enabled();

  String command();

  /** Vazio significa o modelo padrão do próprio CLI. */
  String model();

  Duration timeout();

  /** Esforço das etapas sem valor próprio. */
  String effort();

  Map<String, String> efforts();

  default String effortFor(String task) {
    return LocalCliEfforts.serverEffort(task, efforts(), effort());
  }

  default String effortFor(String task, Map<String, String> accountEfforts) {
    return LocalCliEfforts.effort(task, efforts(), effort(), accountEfforts);
  }

  default Map<String, String> serverEfforts() {
    return LocalCliEfforts.serverEfforts(efforts(), effort());
  }
}
