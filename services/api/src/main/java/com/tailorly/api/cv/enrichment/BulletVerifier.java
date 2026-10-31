package com.tailorly.api.cv.enrichment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verificador determinístico das frases que a IA reescreveu. Não confia na IA: uma frase só passa
 * se (1) cita pelo menos uma fonte existente; (2) todo número dela aparece nas fontes citadas;
 * (3) todo nome próprio (palavra com maiúscula fora do início de frase) aparece nas fontes citadas.
 * Isso impede que um número, uma ferramenta ou um resultado inventado chegue ao currículo.
 */
final class BulletVerifier {
  private static final Pattern NUMBER = Pattern.compile("\\d+");
  private static final Pattern PROPER_NAME = Pattern.compile("\\p{Lu}[\\p{L}\\p{N}+#.\\-]*");
  private static final Pattern SENTENCE_START = Pattern.compile(".*[.!?:;\"“(]\\s*$");

  private BulletVerifier() {
  }

  /** Uma frase proposta pela IA e as fontes (ids) que ela diz ter usado. */
  record ProposedBullet(String text, List<String> anchors) {
  }

  /** Frases aceitas (com o texto normalizado), na ordem proposta, e quantas foram rejeitadas. */
  record Verdict(List<ProposedBullet> kept, int rejected) {
  }

  /**
   * @param sourceTexts texto de cada fonte disponível para o projeto, pelo id
   */
  static Verdict verify(List<ProposedBullet> proposed, Map<String, String> sourceTexts, int maxBullets,
      int maxChars) {
    List<ProposedBullet> kept = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    int rejected = 0;
    for (ProposedBullet bullet : proposed) {
      String text = normalize(bullet.text());
      if (kept.size() < maxBullets && isGrounded(text, bullet.anchors(), sourceTexts, maxChars)
          && seen.add(text.toLowerCase(Locale.ROOT))) {
        kept.add(new ProposedBullet(text, bullet.anchors()));
      } else {
        rejected++;
      }
    }
    return new Verdict(List.copyOf(kept), rejected);
  }

  private static boolean isGrounded(String text, List<String> anchors, Map<String, String> sourceTexts,
      int maxChars) {
    if (text.isEmpty() || text.length() > maxChars || anchors == null || anchors.isEmpty()) {
      return false;
    }
    StringBuilder cited = new StringBuilder();
    for (String anchor : anchors) {
      if (!sourceTexts.containsKey(anchor)) {
        return false;
      }
      cited.append(sourceTexts.get(anchor)).append('\n');
    }
    String citedText = cited.toString();

    Set<String> citedNumbers = new HashSet<>();
    Matcher citedMatcher = NUMBER.matcher(citedText);
    while (citedMatcher.find()) {
      citedNumbers.add(citedMatcher.group());
    }
    Matcher numbers = NUMBER.matcher(text);
    while (numbers.find()) {
      if (!citedNumbers.contains(numbers.group())) {
        return false;
      }
    }

    String citedLower = citedText.toLowerCase(Locale.ROOT);
    Matcher names = PROPER_NAME.matcher(text);
    while (names.find()) {
      String name = names.group().replaceAll("\\.+$", "");
      boolean atSentenceStart = names.start() == 0 || SENTENCE_START.matcher(text.substring(0, names.start())).matches();
      if (atSentenceStart || name.length() < 2) {
        continue;
      }
      if (!citedLower.contains(name.toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  static String normalize(String text) {
    return text == null ? "" : text.replaceAll("\\s+", " ").strip();
  }
}
