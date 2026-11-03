package com.tailorly.api.cv.enrichment;

import java.util.UUID;

import com.tailorly.api.cv.evidence.SupplementaryEvidence;

/**
 * Evidência confirmada pela pessoa, no formato mínimo que o enriquecimento precisa. {@code subjectKey}
 * é o id da entrada do currículo a que ela se refere; as antigas só têm {@code subjectUrl}, o link do
 * projeto. {@code excerpt} é o trecho literal da fonte, {@code sourceKind}, de onde veio, e {@code claim}, o
 * detalhe que a pessoa confirmou. Evidências de projetos que não estão no Currículo Base (repositórios do
 * GitHub) têm {@code subjectKey} iniciado por {@link #CANDIDATE_PREFIX} e {@code subjectLabel} com o nome.
 */
public record ConfirmedEvidence(UUID id, String subjectUrl, String excerpt, String subjectKey, String sourceKind,
    String subjectLabel, String claim) {
  public static final String CONFIRMED = "confirmado";
  /** Prefixo da chave de um projeto candidato, que ainda não faz parte do Currículo Base. */
  public static final String CANDIDATE_PREFIX = "github:";

  public ConfirmedEvidence(UUID id, String subjectUrl, String excerpt) {
    this(id, subjectUrl, excerpt, null, null, null, null);
  }

  public ConfirmedEvidence(UUID id, String subjectUrl, String excerpt, String subjectKey, String sourceKind) {
    this(id, subjectUrl, excerpt, subjectKey, sourceKind, null, null);
  }

  public static ConfirmedEvidence from(SupplementaryEvidence evidence) {
    return new ConfirmedEvidence(evidence.getId(), evidence.getSubjectUrl(), evidence.getSourceExcerpt(),
        evidence.getSubjectKey(), evidence.getSourceKind(), evidence.getSubjectLabel(), evidence.getClaim());
  }

  public boolean isCandidateProject() {
    return subjectKey != null && subjectKey.startsWith(CANDIDATE_PREFIX);
  }
}
