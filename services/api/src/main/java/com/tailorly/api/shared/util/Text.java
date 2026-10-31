package com.tailorly.api.shared.util;

import java.util.Locale;

/** Normalização de texto para comparar trechos e termos sem diferença de espaços ou de caixa. */
public final class Text {
  private Text() {
  }

  /** Espaços colapsados e bordas removidas; nulo vira vazio. */
  public static String normalize(String text) {
    return text == null ? "" : text.replaceAll("\\s+", " ").strip();
  }

  /** Verdadeiro quando {@code term} aparece em {@code text}, ignorando espaços e caixa. */
  public static boolean mentions(String text, String term) {
    String needle = normalize(term).toLowerCase(Locale.ROOT);
    return !needle.isEmpty() && normalize(text).toLowerCase(Locale.ROOT).contains(needle);
  }
}
