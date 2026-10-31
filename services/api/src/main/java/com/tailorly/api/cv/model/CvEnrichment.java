package com.tailorly.api.cv.model;

/**
 * O que o enriquecimento com evidências confirmadas fez numa geração: {@code status} é
 * {@code completed}, {@code skipped} (nenhuma evidência confirmada casou com um projeto),
 * {@code unavailable} (provedor {@code rules}) ou {@code failed} (a IA falhou e o texto original foi mantido).
 * Frases rejeitadas pelo verificador não aparecem no currículo, mas são contadas aqui para auditoria.
 */
public record CvEnrichment(String status, int bulletsKept, int bulletsRejected, int evidenceUsed) {
}
