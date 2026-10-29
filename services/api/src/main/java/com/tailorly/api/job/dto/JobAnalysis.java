package com.tailorly.api.job.dto;

import java.util.List;

import lombok.Builder;

/**
 * Leitura semântica de uma versão de vaga feita por IA. Todo requisito traz um trecho copiado
 * literalmente do texto da vaga ({@code evidence}); itens sem esse trecho são descartados.
 *
 * <p>{@code seniority}: {@code intern}, {@code junior}, {@code mid}, {@code senior}, {@code staff},
 * {@code lead}; nulo quando a vaga não declara. {@code importance}: {@code required} ou
 * {@code preferred}. {@code category}: {@code technology}, {@code experience}, {@code education},
 * {@code language}, {@code certification}, {@code soft_skill} ou {@code other}.</p>
 */
@Builder(toBuilder = true)
public record JobAnalysis(
    String seniority,
    String seniorityEvidence,
    List<Requirement> requirements,
    List<String> technologies,
    String analyzerVersion
) {
  public record Requirement(
      String text,
      String importance,
      String category,
      Integer minYears,
      List<String> technologies,
      String evidence
  ) {
  }
}
