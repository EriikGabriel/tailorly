package com.tailorly.api.cv.model;

import java.util.List;

/**
 * O que a vaga influenciou numa geração: os termos que ela pediu e quais deles já constavam nos
 * fatos aprovados. Serve para auditoria; termos da vaga sem fato aprovado não vão ao currículo.
 * {@code enrichment} é nulo em gerações anteriores à fase de enriquecimento. {@code language} é o idioma
 * efetivo do currículo ({@code pt-BR} ou {@code en}); {@code projectsAdded} e {@code projectsOmitted} listam
 * os projetos incluídos a partir de evidências confirmadas (ex.: repositórios do GitHub) e os projetos do
 * Currículo Base deixados de fora desta versão. Os três são nulos em gerações antigas.
 */
public record CvTailoring(List<String> jobTerms, List<String> matchedTerms, String analysisStatus,
    CvEnrichment enrichment, String language, List<String> projectsAdded, List<String> projectsOmitted) {
  public CvTailoring(List<String> jobTerms, List<String> matchedTerms, String analysisStatus,
      CvEnrichment enrichment) {
    this(jobTerms, matchedTerms, analysisStatus, enrichment, null, null, null);
  }
}
