package com.tailorly.api.job.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/** Converte HTML de descrição de vaga em linhas de texto, preservando listas e títulos. */
final class HtmlText {
  /** Linha de texto; {@code emphasized} indica texto todo em negrito ou título, {@code heading} só {@code h1}-{@code h6}. */
  record Line(String text, boolean emphasized, boolean heading) {
  }

  private static final Set<String> BLOCKS = Set.of(
      "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "tr", "section", "article",
      "table", "blockquote", "header", "footer", "main", "br", "hr", "dl", "dt", "dd", "pre");
  private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
  private static final Set<String> EMPHASIS = Set.of("h1", "h2", "h3", "h4", "h5", "h6", "strong", "b");

  // Alguns editores removem o <li>/<br> e deixam os itens colados ("Java.Proficiência em React");
  // um ponto sem espaço antes de maiúscula+minúsculas separa frases sem quebrar "Node.js" ou "ASP.NET".
  // Depois de sigla ("CI/CD.Familiaridade") exige-se palavra de 4+ letras para preservar "ASP.Net".
  private static final Pattern GLUED_SENTENCES = Pattern.compile(
      "(?<=[\\p{Ll}\\d)])\\.(?=\\p{Lu}\\p{Ll}{2,})|(?<=\\p{Lu}{2})\\.(?=\\p{Lu}\\p{Ll}{3,})");
  private static final Pattern LEADING_BULLET = Pattern.compile("^[\\s•●▪◦·\\-–—*►✓✔→>]+");

  private HtmlText() {
  }

  /** Aceita HTML (inclusive escapado como texto) ou texto puro. */
  static List<Line> fromMarkupOrText(String value) {
    if (value == null || value.isBlank())
      return List.of();
    String markup = value.contains("&lt;") && !value.contains("<") ? Parser.unescapeEntities(value, false) : value;
    if (markup.indexOf('<') >= 0)
      return lines(Jsoup.parseBodyFragment(markup).body());
    return plainLines(markup);
  }

  static List<Line> plainLines(String text) {
    List<Line> lines = new ArrayList<>();
    for (String raw : text.split("\\R"))
      addSplit(lines, raw, false, false);
    return lines;
  }

  static List<Line> lines(Element root) {
    LineCollector collector = new LineCollector();
    NodeTraversor.traverse(new NodeVisitor() {
      @Override
      public void head(Node node, int depth) {
        if (node instanceof TextNode textNode) {
          collector.text(textNode.text());
        } else if (node instanceof Element element) {
          if (BLOCKS.contains(element.normalName()))
            collector.endLine();
          if (EMPHASIS.contains(element.normalName()))
            collector.emphasisDepth++;
          if (HEADINGS.contains(element.normalName()))
            collector.headingDepth++;
        }
      }

      @Override
      public void tail(Node node, int depth) {
        if (node instanceof Element element) {
          if (EMPHASIS.contains(element.normalName()))
            collector.emphasisDepth--;
          if (HEADINGS.contains(element.normalName()))
            collector.headingDepth--;
          if (BLOCKS.contains(element.normalName()))
            collector.endLine();
        }
      }
    }, root);
    collector.endLine();
    return collector.lines;
  }

  static String stripBullet(String text) {
    return LEADING_BULLET.matcher(text).replaceFirst("").trim();
  }

  static String join(List<Line> lines) {
    StringBuilder out = new StringBuilder();
    for (Line line : lines) {
      if (!out.isEmpty())
        out.append('\n');
      out.append(line.text());
    }
    return out.toString();
  }

  private static void addSplit(List<Line> target, String raw, boolean emphasized, boolean heading) {
    String cleaned = raw.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    if (cleaned.isEmpty())
      return;
    String[] pieces = GLUED_SENTENCES.split(cleaned);
    for (int i = 0; i < pieces.length; i++) {
      String text = pieces[i].trim();
      if (text.isEmpty())
        continue;
      // O ponto consumido pelo split volta ao fim de cada frase, exceto da última.
      boolean single = pieces.length == 1;
      target.add(new Line(i < pieces.length - 1 ? text + "." : text, emphasized && single, heading && single));
    }
  }

  private static final class LineCollector {
    final List<Line> lines = new ArrayList<>();
    final StringBuilder current = new StringBuilder();
    int emphasisDepth;
    int headingDepth;
    boolean hasPlain;
    boolean hasEmphasized;
    boolean hasOutsideHeading;

    void text(String text) {
      current.append(text);
      if (text.isBlank())
        return;
      if (emphasisDepth > 0)
        hasEmphasized = true;
      else
        hasPlain = true;
      if (headingDepth == 0)
        hasOutsideHeading = true;
    }

    void endLine() {
      addSplit(lines, current.toString(), hasEmphasized && !hasPlain, hasEmphasized && !hasOutsideHeading);
      current.setLength(0);
      hasPlain = false;
      hasEmphasized = false;
      hasOutsideHeading = false;
    }
  }
}
