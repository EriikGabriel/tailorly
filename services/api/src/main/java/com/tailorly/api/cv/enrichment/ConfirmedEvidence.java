package com.tailorly.api.cv.enrichment;

import java.util.UUID;

import com.tailorly.api.cv.evidence.SupplementaryEvidence;

/**
 * Evidência confirmada pela pessoa, no formato mínimo que o enriquecimento precisa. {@code subjectUrl}
 * é o link do projeto a que ela se refere; {@code excerpt} é o trecho literal da fonte.
 */
public record ConfirmedEvidence(UUID id, String subjectUrl, String excerpt) {
  public static final String CONFIRMED = "confirmado";

  public static ConfirmedEvidence from(SupplementaryEvidence evidence) {
    return new ConfirmedEvidence(evidence.getId(), evidence.getSubjectUrl(), evidence.getSourceExcerpt());
  }
}
