package com.tailorly.api.extraction.interpreting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Localiza no texto da página o trecho de evidência citado pelo modelo. */
final class ExcerptLocator {

  /**
   * Índices {@code [início, fim)} do trecho na página, ou {@code null} se ele não existir. Aceita
   * diferenças de espaço e quebra de linha entre o trecho do modelo e o texto extraído do PDF.
   */
  static int[] locate(String pageText, String excerpt) {
    int exact = pageText.indexOf(excerpt);
    if (exact >= 0)
      return new int[] {exact, exact + excerpt.length()};

    String[] tokens = excerpt.trim().split("\\s+");
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < tokens.length; i++) {
      if (i > 0)
        regex.append("\\s+");
      regex.append(Pattern.quote(tokens[i]));
    }

    Matcher matcher = Pattern.compile(regex.toString()).matcher(pageText);
    return matcher.find() ? new int[] {matcher.start(), matcher.end()} : null;
  }

  private ExcerptLocator() {
  }
}
