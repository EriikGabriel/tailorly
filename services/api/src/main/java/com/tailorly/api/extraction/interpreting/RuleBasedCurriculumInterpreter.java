package com.tailorly.api.extraction.interpreting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Extrai somente dados de contato explicitamente presentes no texto da página, sem IA.
 *
 * <p>Estratégia interna de {@link RoutingCurriculumInterpreter} para o provider {@code rules};
 * não implementa {@link CurriculumInterpreter} diretamente porque não depende de owner.</p>
 */
@Component
public class RuleBasedCurriculumInterpreter {
  private static final List<Rule> RULES = List.of(
      new Rule("email", Pattern.compile(
          "(?<![\\w.+-])[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}(?![\\w.-])")),
      new Rule("phone", Pattern.compile(
          "(?i)(?:tel(?:efone)?|cel(?:ular)?|phone|mobile)\\s*[:：-]?\\s*(\\+?\\d[0-9() .-]{7,30}\\d)")));

  public List<FactProposal> interpret(List<ExtractedTextPage> pages) {
    List<FactProposal> proposals = new ArrayList<>();
    
    for (ExtractedTextPage page : pages) {
      if (page.text() == null || page.text().isBlank())
        continue;

      proposals.addAll(interpretPage(page));
    }

    return List.copyOf(proposals);
  }

  private List<FactProposal> interpretPage(ExtractedTextPage page) {
    List<FactProposal> proposals = new ArrayList<>();

    for (Rule rule : RULES) {
      Matcher matcher = rule.pattern().matcher(page.text());

      while (matcher.find()) {
        proposals.add(toProposal(page, rule, matcher));
      }
    }

    return proposals;
  }

  private FactProposal toProposal(ExtractedTextPage page, Rule rule, Matcher matcher) {
    int start = matcher.groupCount() == 0 ? matcher.start() : matcher.start(1);
    int end = matcher.groupCount() == 0 ? matcher.end() : matcher.end(1);

    String matchedText = page.text().substring(start, end);
    String excerpt = matchedText.strip();

    start += matchedText.length() - matchedText.stripLeading().length();

    return new FactProposal("identity", rule.fieldKey(), excerpt,
        page.extractedPageId(), excerpt, start, start + excerpt.length());
  }

  private record Rule(String fieldKey, Pattern pattern) {
  }
}
