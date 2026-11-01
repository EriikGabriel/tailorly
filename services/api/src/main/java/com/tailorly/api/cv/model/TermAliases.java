package com.tailorly.api.cv.model;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Grafias equivalentes de uma tecnologia ("React.js", "ReactJS" e "React"; "PostgreSQL" e "Postgres").
 * Uma vaga e um currículo raramente escrevem o mesmo nome do mesmo jeito, e comparar só a grafia
 * exata faz uma tecnologia presente parecer ausente.
 */
public final class TermAliases {
  private static final Map<String, Set<String>> EQUIVALENTS = Map.ofEntries(
      Map.entry("postgresql", Set.of("postgres", "postgre")),
      Map.entry("postgres", Set.of("postgresql")),
      Map.entry("kubernetes", Set.of("k8s")),
      Map.entry("k8s", Set.of("kubernetes")),
      Map.entry("javascript", Set.of("js")),
      Map.entry("typescript", Set.of("ts")),
      Map.entry("spring framework", Set.of("spring", "spring boot")),
      Map.entry("spring boot", Set.of("spring")),
      Map.entry("golang", Set.of("go")),
      Map.entry("mongodb", Set.of("mongo")),
      Map.entry("c#", Set.of("csharp", "c sharp")),
      Map.entry("ci/cd", Set.of("ci cd", "cicd")));
  private static final Set<String> JS_LIBRARIES = Set.of("react", "vue", "node", "next", "nuxt", "express", "nest", "angular");

  private TermAliases() {
  }

  /** O termo e suas grafias equivalentes, já sem acento e em minúsculas. */
  public static Set<String> of(String term) {
    String key = fold(term);
    Set<String> aliases = new LinkedHashSet<>();
    aliases.add(key);
    String base = key.replaceAll("[.\\s-]?js$", "");
    if (!base.equals(key) && JS_LIBRARIES.contains(base)) {
      aliases.add(base);
    }
    if (JS_LIBRARIES.contains(base)) {
      aliases.add(base);
      aliases.add(base + ".js");
      aliases.add(base + "js");
    }
    aliases.addAll(EQUIVALENTS.getOrDefault(key, Set.of()));
    return aliases;
  }

  /** Verdadeiro quando o texto cita o termo, ou uma grafia equivalente, como palavra inteira. */
  public static boolean mentions(String text, String term) {
    String folded = fold(text);
    return of(term).stream().anyMatch(alias -> Pattern.compile(
        "(?<![\\p{L}\\p{N}])" + Pattern.quote(alias) + "(?![\\p{L}\\p{N}])").matcher(folded).find());
  }

  static String fold(String text) {
    return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT).strip();
  }
}
