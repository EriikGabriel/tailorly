package com.tailorly.api.cv.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Preferências da pessoa para a geração: diretrizes de destaque (só orientam a ordem do que já está
 * aprovado) e se a esteira deve iterar o layout até a nota de ATS ficar boa.
 */
public record CvPreferences(
    @NotNull @Size(max = 10) List<@Size(min = 1, max = 80) String> guidelines,
    boolean optimizeForAts
) {
  public static CvPreferences defaults() {
    return new CvPreferences(List.of(), true);
  }
}
