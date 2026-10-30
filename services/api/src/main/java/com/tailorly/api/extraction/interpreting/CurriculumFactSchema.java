package com.tailorly.api.extraction.interpreting;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Esquema de fatos aceito pela revisão: categorias e chaves de campo que a interface sabe exibir
 * e editar. Tudo fora dele é descartado antes de virar proposta.
 */
final class CurriculumFactSchema {

  private static final Map<String, Set<String>> LIST_FIELDS = Map.of(
      "experience", Set.of("company", "role", "employment", "status", "start", "end", "description"),
      "volunteering", Set.of("organization", "role", "status", "start", "end", "description"),
      "projects", Set.of("title", "context", "statement", "metric", "link"),
      "degrees", Set.of("degree", "institution", "status", "start", "end"),
      "certifications", Set.of("name", "issuer", "credential", "issued", "expires"),
      "languages", Set.of("language", "proficiency"),
      "technologies", Set.of("name", "context"));
  private static final Set<String> IDENTITY_FIELDS = Set.of(
      "name", "headline", "location", "email", "phone", "linkedin", "github", "website", "summary");
  private static final Pattern ITEM_PREFIX = Pattern.compile("^(\\d{1,3}):(.+)$");

  /** Categoria em minúsculas se for conhecida; {@code null} caso contrário. */
  static String normalizeCategory(String category) {
    if (category == null)
      return null;
    String normalized = category.trim().toLowerCase();
    return "identity".equals(normalized) || LIST_FIELDS.containsKey(normalized) ? normalized : null;
  }

  // Só categorias e chaves que a revisão sabe exibir e editar são aceitas; o restante é descartado.
  static String normalizeFieldKey(String category, String fieldKey) {
    if (category == null || fieldKey == null)
      return null;

    String key = fieldKey.trim();
    Matcher prefix = ITEM_PREFIX.matcher(key);
    String index = null;
    if (prefix.matches()) {
      index = prefix.group(1);
      key = prefix.group(2).trim();
    }

    if ("identity".equals(category))
      return IDENTITY_FIELDS.contains(key) ? key : null;

    if (!LIST_FIELDS.get(category).contains(key))
      return null;
    // Sem índice o item é tratado como o primeiro; o modelo deve sempre informá-lo.
    return (index == null ? "0" : index) + ":" + key;
  }

  private CurriculumFactSchema() {
  }
}
