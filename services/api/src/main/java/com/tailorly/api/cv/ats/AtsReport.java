package com.tailorly.api.cv.ats;

import java.util.List;

/**
 * Avaliação de triagem automática (ATS) de um currículo gerado. É uma estimativa por regras, não o
 * resultado de um ATS real: mede cobertura dos termos da vaga, estrutura, leitura do texto extraído
 * do próprio PDF e tamanho.
 *
 * <p>{@code missingTerms} são termos da vaga que não aparecem no texto. A maioria é algo que o
 * Currículo Base não afirma; a esteira nunca os acrescenta.</p>
 */
public record AtsReport(
    int score,
    List<AtsCheck> checks,
    List<String> matchedTerms,
    List<String> missingTerms,
    Integer pages,
    int words
) {
  public record AtsCheck(String id, String label, int points, int maxPoints, String detail) {
  }
}
