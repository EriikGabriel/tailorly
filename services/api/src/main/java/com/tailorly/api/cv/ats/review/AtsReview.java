package com.tailorly.api.cv.ats.review;

import java.time.Instant;
import java.util.List;

/**
 * Segunda opinião da IA sobre o currículo gerado, lendo só o texto que um parser extrai do PDF. Não
 * substitui a nota por regras: serve para explicar, requisito por requisito, o que a vaga pede e o
 * texto sustenta ou não. {@code status}: {@code completed}, {@code unavailable} (provedor
 * {@code rules}) ou {@code failed}. {@code alignment} (0–100) pondera obrigatórios o dobro e conta
 * "parcial" como meio.
 */
public record AtsReview(
    String status,
    String summary,
    int alignment,
    List<RequirementCheck> requirements,
    List<String> strengths,
    List<String> suggestions,
    Instant reviewedAt
) {
  /**
   * {@code status}: {@code met}, {@code partial} ou {@code missing}. {@code evidence} é um trecho do
   * currículo conferido por código; sem ele, o requisito nunca conta como atendido.
   */
  public record RequirementCheck(String requirement, String importance, String status, String evidence,
      String note) {
  }

  public static AtsReview unavailable(String status) {
    return new AtsReview(status, null, 0, List.of(), List.of(), List.of(), Instant.now());
  }
}
