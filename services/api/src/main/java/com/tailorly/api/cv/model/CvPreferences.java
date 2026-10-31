package com.tailorly.api.cv.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Preferências da pessoa para a geração: diretrizes de destaque (só orientam a ordem do que já está
 * aprovado), se a esteira deve iterar o layout até a nota de ATS ficar boa e o template de layout
 * (nome de um template embarcado; nulo usa o padrão). O template só muda a apresentação.
 */
public record CvPreferences(
    @NotNull @Size(max = 10) List<@Size(min = 1, max = 80) String> guidelines,
    boolean optimizeForAts,
    @Size(max = 64) String templateName
) {
  public CvPreferences(List<String> guidelines, boolean optimizeForAts) {
    this(guidelines, optimizeForAts, null);
  }

  public static CvPreferences defaults() {
    return new CvPreferences(List.of(), true, null);
  }
}
