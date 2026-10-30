package com.tailorly.api.job.reader;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Distribui as linhas de uma descrição entre responsabilidades, requisitos, diferenciais,
 * benefícios etc. a partir de títulos em português e inglês. Um título sozinho na linha muda a
 * seção corrente; um rótulo seguido de conteúdo ("Diferenciais: Kafka") classifica só aquele item.
 */
final class JobSectionClassifier {
  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final int MAX_HEADING_WORDS = 8;
  private static final int MAX_HEADING_LENGTH = 80;
  private static final int MAX_LABEL_LENGTH = 60;
  private static final int MAX_UNKNOWN_HEADING_LENGTH = 100;

  private JobSectionClassifier() {
  }

  static void classify(List<HtmlText.Line> lines, JobSection defaultSection, JobSections out) {
    JobSection current = defaultSection;

    for (HtmlText.Line line : lines) {
      String text = HtmlText.stripBullet(line.text());
      if (text.isEmpty())
        continue;

      int colon = text.indexOf(':');
      if (colon > 0 && colon <= MAX_LABEL_LENGTH) {
        JobSection labelled = sectionFor(text.substring(0, colon));
        if (labelled != null) {
          String rest = text.substring(colon + 1).trim();
          if (rest.isEmpty())
            current = labelled;
          else
            out.add(labelled, rest);
          continue;
        }
      }

      JobSection heading = headingSection(text, line.emphasized());
      if (heading != null) {
        current = heading;
        continue;
      }
      // Um título real (h1-h6) sem palavra-chave conhecida encerra a seção anterior, para que o texto
      // institucional que costuma vir depois dos benefícios não seja listado como benefício.
      if (line.heading() && text.length() <= MAX_UNKNOWN_HEADING_LENGTH) {
        current = JobSection.OTHER;
        continue;
      }
      out.add(current, text);
    }
  }

  private static JobSection headingSection(String text, boolean emphasized) {
    if (text.length() > MAX_HEADING_LENGTH || text.endsWith("."))
      return null;
    String trimmed = text.replaceAll("[:?]+$", "").trim();
    int words = trimmed.split("\\s+").length;
    boolean punctuated = text.endsWith("?");
    if (words > MAX_HEADING_WORDS || !(emphasized || punctuated || words <= 3))
      return null;
    return sectionFor(trimmed);
  }

  static JobSection sectionFor(String label) {
    String normalized = " " + normalize(label) + " ";
    if (normalized.isBlank())
      return null;

    if (containsAny(normalized, "beneficio", " benefits ", " perks ", "oferecemos", "what we offer"))
      return JobSection.BENEFITS;
    if (containsAny(normalized, "etapas", "processo seletivo", "hiring process", "interview process",
        "como funciona o processo"))
      return JobSection.HIRING_PROCESS;
    if (containsAny(normalized, "desejav", "diferencia", " nice to have ", " preferred ", " plus "))
      return JobSection.NICE_TO_HAVE;
    if (containsAny(normalized, "responsabilidade", "atribuic", "atividades", "o que voce vai fazer",
        "o que voce fara", "what you will do", "what you ll do", "responsibilities", " your role ",
        "seu dia a dia", "day to day"))
      return JobSection.RESPONSIBILITIES;
    if (containsAny(normalized, "requisito", "qualificac", "pre requisito", "requirements", "qualifications",
        "o que esperamos", "o que buscamos", "o que procuramos", "what we are looking for", "what we re looking for", "what you ll need",
        "what you need", "must have", " you have "))
      return JobSection.REQUIREMENTS;
    if (containsAny(normalized, "sobre a empresa", "sobre nos", "quem somos", "about us", "about the company",
        "who we are"))
      return JobSection.ABOUT_COMPANY;
    return null;
  }

  private static boolean containsAny(String normalized, String... needles) {
    for (String needle : needles) {
      if (normalized.contains(needle))
        return true;
    }
    return false;
  }

  private static String normalize(String value) {
    String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "");
    return NON_ALPHANUMERIC.matcher(decomposed).replaceAll(" ").trim();
  }
}
