package com.tailorly.api.cv.ats.review;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
    @Schema(types = {"string", "null"}) String summary,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int alignment,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RequirementCheck> requirements,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> strengths,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> suggestions,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant reviewedAt
) {
  /**
   * {@code status}: {@code met}, {@code partial} ou {@code missing}. {@code evidence} é um trecho do
   * currículo conferido por código; sem ele, o requisito nunca conta como atendido.
   */
  public record RequirementCheck(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String requirement, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String importance, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
      @Schema(types = {"string", "null"}) String evidence, @Schema(types = {"string", "null"}) String note) {
  }

  public static AtsReview unavailable(String status) {
    return new AtsReview(status, null, 0, List.of(), List.of(), List.of(), Instant.now());
  }
}
