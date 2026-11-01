package com.tailorly.api.base.version;

/**
 * Estados de uma versão do Currículo Base em que os fatos ainda podem ser revisados. Uma versão
 * {@code ACTIVE} continua editável: o snapshot ativo é uma cópia congelada dos fatos aprovados,
 * então corrigir a versão não altera o que as gerações leem até uma nova aprovação trocar o snapshot.
 */
public final class BaseVersionStatus {
  public static final java.util.List<String> REVIEWABLE = java.util.List.of("CANDIDATE", "EXTRACTED", "ACTIVE");

  private BaseVersionStatus() {
  }

  public static boolean isReviewable(String status) {
    return status != null && REVIEWABLE.contains(status);
  }
}
