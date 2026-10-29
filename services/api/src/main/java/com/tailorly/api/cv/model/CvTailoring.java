package com.tailorly.api.cv.model;

import java.util.List;

/**
 * O que a vaga influenciou numa geração: os termos que ela pediu e quais deles já constavam nos
 * fatos aprovados. Serve para auditoria; termos da vaga sem fato aprovado não vão ao currículo.
 */
public record CvTailoring(List<String> jobTerms, List<String> matchedTerms, String analysisStatus) {
}
