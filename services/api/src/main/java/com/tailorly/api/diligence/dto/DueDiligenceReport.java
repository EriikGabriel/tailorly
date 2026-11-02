package com.tailorly.api.diligence.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Relatório gravado de uma due diligence. É um registro histórico imutável: uma nova análise cria outro
 * relatório. Todo item de {@code factors} corresponde a um dos onze fatores, na ordem fixa.
 *
 * @param entity entidade analisada (nome e vaga de origem)
 * @param consultedSources tipos de fonte realmente consultados; {@code privateSourcesConsulted} é sempre falso
 *     até existir integração com acesso autorizado
 */
public record DueDiligenceReport(
    Entity entity,
    Instant consultedAt,
    List<String> consultedSources,
    boolean privateSourcesConsulted,
    String analyzerVersion,
    List<FactorResult> factors
) {
  /** Empresa analisada; {@code jobPostingId}/{@code jobVersionNumber} ligam a análise à vaga que a motivou. */
  public record Entity(String name, String website, UUID jobPostingId, Integer jobVersionNumber, String jobTitle) {
  }

  /**
   * Resultado de um fator. {@code status}: {@code evidence_found}, {@code no_data} (nada encontrado, o que
   * não indica problema) ou {@code source_unavailable} (a busca falhou).
   */
  public record FactorResult(
      String factor,
      String status,
      String summary,
      Uncertainty uncertainty,
      List<Finding> findings
  ) {
  }

  /** {@code level}: {@code low}, {@code medium} ou {@code high}. */
  public record Uncertainty(String level, String note) {
  }

  /**
   * Uma conclusão com sua origem. {@code sourceType}: {@code official}, {@code company}, {@code news},
   * {@code review} (relato de pessoas, não comprova o fato) ou {@code other}. {@code polarity}:
   * {@code positive}, {@code negative} ou {@code neutral}. {@code retrievedAt} é a data da consulta; a data
   * de publicação só é conhecida quando consta do trecho. {@code entityConfirmed} é falso quando a fonte
   * pode tratar de outra empresa de nome parecido.
   */
  public record Finding(
      String claim,
      String sourceUrl,
      String sourceTitle,
      String excerpt,
      String sourceType,
      String polarity,
      boolean unverifiedReport,
      boolean entityConfirmed,
      Instant retrievedAt
  ) {
  }
}
