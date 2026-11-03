package com.tailorly.api.cv.model;

/**
 * O que a reescrita de conteúdo fez numa geração: {@code status} é {@code completed},
 * {@code skipped} (nada com texto suficiente para reescrever), {@code unavailable} (provedor
 * {@code rules}) ou {@code failed} (a IA falhou e o texto aprovado foi mantido). Frases rejeitadas pelo
 * verificador, ou descartadas por perderem um número do texto original, não aparecem no currículo,
 * mas são contadas aqui para auditoria. {@code summaryRewritten} indica se o resumo foi reescrito e
 * {@code headlineRewritten}, se o título profissional foi adequado à vaga ({@code headline} traz o título
 * final nesse caso; {@code originalHeadline}, o aprovado no Currículo Base, nulo se não havia).
 */
public record CvEnrichment(String status, int bulletsKept, int bulletsRejected, int evidenceUsed,
    boolean summaryRewritten, boolean headlineRewritten, String headline, String originalHeadline) {
  public CvEnrichment(String status, int bulletsKept, int bulletsRejected, int evidenceUsed,
      boolean summaryRewritten) {
    this(status, bulletsKept, bulletsRejected, evidenceUsed, summaryRewritten, false, null, null);
  }
}
