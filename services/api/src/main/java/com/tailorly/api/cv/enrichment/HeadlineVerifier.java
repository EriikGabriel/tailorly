package com.tailorly.api.cv.enrichment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verificador determinístico do título profissional proposto pela IA para a vaga. Aceita uma mudança de
 * título (por exemplo "Desenvolvedor Full Stack" ou "Software Engineer") só se: (1) cita fontes
 * existentes; (2) é curto e sem números; (3) cada palavra é vocabulário genérico de cargo ou aparece
 * nas fontes do currículo (tecnologias e nomes de ferramentas nunca entram só pela vaga); (4) palavras de
 * senioridade ou de gestão só entram se aparecem nas fontes (a vaga pedir "Sênior" não promove ninguém); e (5)
 * a especialidade do título (front-end, back-end, full stack, mobile, dados, DevOps) tem tecnologia
 * correspondente nas fontes aprovadas.
 */
final class HeadlineVerifier {
  static final int MAX_CHARS = 60;
  private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}\\p{N}+#.\\-/]*");
  private static final Set<String> GENERIC = Set.of("desenvolvedor", "desenvolvedora", "engenheiro",
      "engenheira", "developer", "engineer", "software", "web", "full", "stack", "fullstack", "full-stack",
      "front", "front-end", "frontend", "back", "back-end", "backend", "end", "mobile", "dados", "data", "cloud",
      "devops", "programador", "programadora", "de", "da", "do", "em", "e", "para", "and", "of", "for", "in", "&",
      "-", "/", "|", "aplicacoes", "applications", "sistemas", "systems", "plataforma", "platform", "ia", "ai",
      "analista", "analyst", "qa", "testes", "test", "automation", "automacao", "integracoes", "apis", "api");
  /** Palavras que declaram nível ou posição: só valem se já estiverem nas fontes aprovadas. */
  private static final Set<String> RANK = Set.of("senior", "sr", "pleno", "pl", "junior", "jr", "lead", "lider",
      "staff", "principal", "especialista", "specialist", "arquiteto", "arquiteta", "architect", "gerente",
      "manager", "head", "coordenador", "coordenadora", "tech", "estagiario", "estagiaria", "intern", "trainee");
  /** Especialidade do título → palavras que a sustentam nas fontes. */
  private static final Map<String, List<String>> SPECIALTIES = Map.of(
      "front", List.of("react", "vue", "angular", "svelte", "html", "css", "javascript", "typescript", "next",
          "tailwind", "front"),
      "back", List.of("java", "spring", "node", "python", "django", "flask", "fastapi", ".net", "c#", "php",
          "laravel", "ruby", "rails", "go", "golang", "kotlin", "nest", "express", "sql", "api", "back"),
      "mobile", List.of("android", "ios", "flutter", "react native", "kotlin", "swift", "mobile", "expo"),
      "dados", List.of("sql", "python", "pandas", "spark", "etl", "power bi", "airflow", "dados", "data", "bi"),
      "devops", List.of("docker", "kubernetes", "terraform", "ci/cd", "aws", "azure", "gcp", "devops", "linux",
          "github actions", "jenkins", "ansible"));

  private HeadlineVerifier() {
  }

  /**
   * @param sourceTexts texto de cada fonte do título, pelo id
   * @return o título normalizado, ou {@code null} quando a proposta não passa
   */
  static String verify(String proposed, List<String> anchors, Map<String, String> sourceTexts) {
    String headline = BulletVerifier.normalize(proposed);
    if (headline.isEmpty() || headline.length() > MAX_CHARS || anchors == null || anchors.isEmpty()
        || headline.chars().anyMatch(Character::isDigit)) {
      return null;
    }
    StringBuilder cited = new StringBuilder();
    for (String anchor : anchors) {
      if (!sourceTexts.containsKey(anchor)) {
        return null;
      }
      cited.append(sourceTexts.get(anchor)).append('\n');
    }
    String citedFolded = fold(cited.toString());
    String allFolded = fold(String.join("\n", sourceTexts.values()));
    Set<String> citedWords = words(citedFolded);

    List<String> tokens = new ArrayList<>(words(fold(headline)));
    for (String token : tokens) {
      if (RANK.contains(token)) {
        if (!citedWords.contains(token)) {
          return null;
        }
      } else if (!GENERIC.contains(token) && !citedWords.contains(token)) {
        return null;
      }
    }
    return supportsSpecialty(fold(headline), allFolded) ? headline : null;
  }

  private static boolean supportsSpecialty(String headline, String sources) {
    boolean full = headline.contains("full stack") || headline.contains("fullstack")
        || headline.contains("full-stack");
    boolean front = full || headline.contains("front");
    boolean back = full || headline.contains("back");
    return (!front || supports("front", sources)) && (!back || supports("back", sources))
        && (!mentionsAny(headline, "mobile") || supports("mobile", sources))
        && (!mentionsAny(headline, "dados", "data") || supports("dados", sources))
        && (!mentionsAny(headline, "devops", "cloud") || supports("devops", sources));
  }

  private static boolean mentionsAny(String text, String... words) {
    Set<String> tokens = words(text);
    for (String word : words) {
      if (tokens.contains(word)) {
        return true;
      }
    }
    return false;
  }

  private static boolean supports(String specialty, String sources) {
    Set<String> tokens = words(sources);
    for (String word : SPECIALTIES.get(specialty)) {
      if (word.contains(" ") ? sources.contains(word) : tokens.contains(word)) {
        return true;
      }
    }
    return false;
  }

  private static Set<String> words(String folded) {
    Set<String> result = new java.util.LinkedHashSet<>();
    Matcher matcher = WORD.matcher(folded);
    while (matcher.find()) {
      result.add(matcher.group().replaceAll("[.\\-/]+$", ""));
    }
    return result;
  }

  private static String fold(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }
}
