package com.tailorly.api.cv.enrichment;

import java.util.UUID;

import com.tailorly.api.cv.evidence.SupplementaryEvidence;

/**
 * Evidência confirmada pela pessoa, no formato mínimo que o enriquecimento precisa. {@code subjectKey}
 * é o id da entrada do currículo a que ela se refere; as antigas só têm {@code subjectUrl}, o link do
 * projeto. {@code excerpt} é o trecho literal da fonte e {@code sourceKind}, de onde veio.
 */
public record ConfirmedEvidence(UUID id, String subjectUrl, String excerpt, String subjectKey, String sourceKind) {
  public static final String CONFIRMED = "confirmado";

  public ConfirmedEvidence(UUID id, String subjectUrl, String excerpt) {
    this(id, subjectUrl, excerpt, null, null);
  }

  public static ConfirmedEvidence from(SupplementaryEvidence evidence) {
    return new ConfirmedEvidence(evidence.getId(), evidence.getSubjectUrl(), evidence.getSourceExcerpt(),
        evidence.getSubjectKey(), evidence.getSourceKind());
  }
}
