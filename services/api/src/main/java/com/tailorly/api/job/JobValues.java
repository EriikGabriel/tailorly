package com.tailorly.api.job;

import java.net.URI;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;

/** Leitura defensiva de valores vindos de páginas externas. */
final class JobValues {
  private JobValues() {
  }

  static String text(JsonNode node, String field) {
    return text(node.path(field));
  }

  static String text(JsonNode node) {
    if (node == null || !(node.isTextual() || node.isNumber()))
      return null;
    return blankToNull(node.asText());
  }

  static String blankToNull(String value) {
    if (value == null)
      return null;
    String trimmed = value.replace(' ', ' ').trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /** Só links http(s) são devolvidos, para que a interface nunca receba {@code javascript:} e similares. */
  static String httpUrl(String value) {
    String candidate = blankToNull(value);
    if (candidate == null)
      return null;
    try {
      URI uri = URI.create(candidate);
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null ? candidate : null;
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  static String normalizeWorkModel(String raw) {
    String value = blankToNull(raw);
    if (value == null)
      return null;
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.contains("remote") || lower.contains("remoto") || lower.contains("telecommute"))
      return "remote";
    if (lower.contains("hybrid") || lower.contains("hibrido") || lower.contains("híbrido"))
      return "hybrid";
    if (lower.contains("on-site") || lower.contains("onsite") || lower.contains("on_site")
        || lower.contains("presencial"))
      return "onsite";
    return lower;
  }
}
