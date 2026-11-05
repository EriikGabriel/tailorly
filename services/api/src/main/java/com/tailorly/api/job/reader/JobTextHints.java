package com.tailorly.api.job.reader;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deduz título e empresa de um texto colado, quando a pessoa não informou. São regras de texto, não
 * IA: só devolvem o que aparece literalmente no texto, e preferem não devolver nada a chutar.
 */
final class JobTextHints {
  // Palavras genéricas que aparecem depois de "buscamos"/"A ..." e não são nome de cargo nem de empresa.
  private static final Set<String> GENERIC = Set.of(
      "pessoa", "pessoas", "profissional", "profissionais", "candidato", "candidata", "talento", "talentos",
      "time", "equipe", "vaga", "empresa", "area", "área", "plataforma", "nossa", "nosso", "oportunidade",
      "funcao", "função", "posicao", "posição", "cultura", "comunidade", "processo", "programa", "projeto",
      "departamento", "alguem", "alguém", "startup", "hub", "grupo", "marca");

  private static final Pattern TITLE_LABEL = Pattern.compile(
      "(?im)^\\s*(?:cargo|vaga|t[ií]tulo|posi[cç][aã]o|job title|position|role)\\s*:\\s*([^\\n]{3,120})$");
  private static final Pattern TITLE_PHRASE = Pattern.compile(
      "(?i)(?:em busca de|buscamos|procuramos|estamos (?:buscando|procurando|contratando)|"
          + "vaga (?:de|para)|we are looking for|we're looking for|hiring)\\s+"
          + "(?:(?:um\\(a\\)|o\\(a\\)|uma|um|o|a|an?)\\s+)?"
          // Para antes de palavras que começam a descrição (a lookahead não consome o trecho).
          + "([^\\n,.;:!?]{3,80}?)(?=\\s+(?:com|para|que|em|no|na|onde|sendo|with|for|who)\\b|\\s*[,.;:!?\\n]|$)");

  private static final Pattern COMPANY_LABEL = Pattern.compile(
      "(?im)^\\s*(?:empresa|companhia|contratante|company)\\s*:\\s*([^\\n]{2,80})$");
  // Sentença de apresentação: "A Studio Rx é um hub…" ou "Somos a Acme, líder…". Sensível a maiúsculas de propósito.
  private static final Pattern COMPANY_SELF = Pattern.compile(
      "(?:^|[.!?\\n]\\s*)(?:A|O)\\s+([^\\n,.;:!?()]{2,60}?)\\s+(?:é|são)\\s+(?:um|uma|o|a|parte|líder|referência)\\b");
  private static final Pattern COMPANY_WE_ARE = Pattern.compile(
      "(?:^|[.!?\\n]\\s*)Somos\\s+(?:a|o)\\s+([^\\n,.;:!?()]{2,60}?)(?=[,.;:!?\\n]|\\s+(?:um|uma)\\b|$)");

  private JobTextHints() {
  }

  /** Título do cargo a partir de rótulo ("Cargo: …") ou frase de busca ("em busca de um …"); nulo se não achar. */
  static String title(String text) {
    String labelled = firstGroup(TITLE_LABEL, text);
    if (labelled != null)
      return labelled;
    Matcher phrase = TITLE_PHRASE.matcher(text);
    while (phrase.find()) {
      String candidate = clean(phrase.group(1));
      if (candidate != null && !isGeneric(candidate))
        return candidate;
    }
    return null;
  }

  /** Nome da empresa a partir de rótulo ("Empresa: …") ou da apresentação ("A X é um …"); nulo se não achar. */
  static String company(String text) {
    String labelled = firstGroup(COMPANY_LABEL, text);
    if (labelled != null)
      return labelled;
    for (Pattern pattern : List.of(COMPANY_SELF, COMPANY_WE_ARE)) {
      Matcher match = pattern.matcher(text);
      while (match.find()) {
        String candidate = clean(match.group(1));
        // Nome de empresa começa com maiúscula: "A vaga é…" e "O que…" ficam de fora.
        if (candidate != null && Character.isUpperCase(candidate.charAt(0)) && !isGeneric(candidate))
          return candidate;
      }
    }
    return null;
  }

  private static String firstGroup(Pattern pattern, String text) {
    Matcher match = pattern.matcher(text);
    return match.find() ? clean(match.group(1)) : null;
  }

  private static String clean(String value) {
    if (value == null)
      return null;
    String trimmed = value.replaceAll("\\s+", " ").strip();
    return trimmed.length() < 2 ? null : trimmed;
  }

  private static boolean isGeneric(String candidate) {
    String head = candidate.split("\\s+")[0].toLowerCase(Locale.ROOT);
    return GENERIC.contains(head);
  }
}
