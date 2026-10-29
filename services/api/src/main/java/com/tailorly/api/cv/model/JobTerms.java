package com.tailorly.api.cv.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingResponse;

/**
 * Termos da vaga (tecnologias e competências) usados só para ordenar o que a pessoa já tem
 * aprovado no Ground Truth. Um termo da vaga nunca entra no currículo por si só.
 */
public final class JobTerms {
  private static final int MAX_TERMS = 200;
  private static final int MAX_TERM_LENGTH = 60;

  private final Map<String, String> byKey = new LinkedHashMap<>();
  private final java.util.Set<String> required = new java.util.HashSet<>();

  private JobTerms() {
  }

  public static JobTerms of(JobPostingResponse details, JobAnalysis analysis) {
    JobTerms terms = new JobTerms();
    if (analysis != null) {
      terms.addAll(analysis.technologies());
      if (analysis.requirements() != null) {
        analysis.requirements().forEach(requirement -> {
          terms.addAll(requirement.technologies());
          if ("required".equals(requirement.importance()) && requirement.technologies() != null) {
            requirement.technologies().stream().filter(java.util.Objects::nonNull)
                .forEach(technology -> terms.required.add(fold(technology.strip())));
          }
        });
      }
    }
    if (details != null) {
      terms.addAll(details.skills());
    }
    return terms;
  }

  /** Termos da vaga na grafia original, sem repetições. */
  public List<String> terms() {
    return List.copyOf(byKey.values());
  }

  /** Peso do termo na avaliação: 2 quando a vaga o declara obrigatório, 1 nos demais. */
  public int weight(String term) {
    return required.contains(fold(term.strip())) ? 2 : 1;
  }

  /** Verdadeiro quando {@code text} contém algum termo da vaga como palavra inteira. */
  public boolean matches(String text) {
    return !matchedIn(text).isEmpty();
  }

  /** Termos da vaga (grafia original) que aparecem em {@code text} como palavra inteira. */
  public List<String> matchedIn(String text) {
    List<String> matched = new ArrayList<>();
    if (text == null || text.isBlank()) {
      return matched;
    }
    String folded = fold(text);
    for (Map.Entry<String, String> term : byKey.entrySet()) {
      if (Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term.getKey()) + "(?![\\p{L}\\p{N}])")
          .matcher(folded).find()) {
        matched.add(term.getValue());
      }
    }
    return matched;
  }

  private void addAll(List<String> values) {
    if (values == null) {
      return;
    }
    for (String value : values) {
      if (value == null || byKey.size() >= MAX_TERMS) {
        continue;
      }
      String term = value.strip();
      if (term.length() >= 2 && term.length() <= MAX_TERM_LENGTH) {
        byKey.putIfAbsent(fold(term), term);
      }
    }
  }

  static String fold(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT);
  }
}
