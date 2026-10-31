package com.tailorly.api.cv.model;

/**
 * O que a reescrita de conteúdo fez numa geração: {@code status} é {@code completed},
 * {@code skipped} (nada com texto suficiente para reescrever), {@code unavailable} (provedor
 * {@code rules}) ou {@code failed} (a IA falhou e o texto aprovado foi mantido). Frases rejeitadas pelo
 * verificador, ou descartadas por perderem um número do texto original, não aparecem no currículo,
 * mas são contadas aqui para auditoria. {@code summaryRewritten} indica se o resumo foi reescrito.
 */
public record CvEnrichment(String status, int bulletsKept, int bulletsRejected, int evidenceUsed,
    boolean summaryRewritten) {
}
