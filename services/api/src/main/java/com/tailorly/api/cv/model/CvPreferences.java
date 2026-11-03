package com.tailorly.api.cv.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Preferências da pessoa para a geração: diretrizes de destaque (dão ênfase à reescrita e à ordem do que já está
 * aprovado), se a esteira deve iterar o layout até a nota de ATS ficar boa, o template de layout
 * (nome de um template embarcado; nulo usa o padrão) e o idioma do currículo final. O template só muda a
 * apresentação. {@code language} é {@code auto} (detecta pela vaga, português ou inglês), {@code pt-BR} ou
 * {@code en}; ausente vale {@code auto}.
 */
public record CvPreferences(
    @NotNull @Size(max = 10) List<@Size(min = 1, max = 80) String> guidelines,
    boolean optimizeForAts,
    @Size(max = 64) String templateName,
    @Pattern(regexp = CvLanguage.PREFERENCE_PATTERN) String language
) {
  public CvPreferences {
    language = CvLanguage.normalizePreference(language);
  }

  public CvPreferences(List<String> guidelines, boolean optimizeForAts, String templateName) {
    this(guidelines, optimizeForAts, templateName, CvLanguage.AUTO);
  }

  public CvPreferences(List<String> guidelines, boolean optimizeForAts) {
    this(guidelines, optimizeForAts, null, CvLanguage.AUTO);
  }

  public static CvPreferences defaults() {
    return new CvPreferences(List.of(), true, null, CvLanguage.AUTO);
  }
}
