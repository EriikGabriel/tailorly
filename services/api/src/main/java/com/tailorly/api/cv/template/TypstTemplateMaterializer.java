package com.tailorly.api.cv.template;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tailorly.api.cv.model.CvDocument;

/**
 * Escreve o conteúdo aprovado direto no corpo do template Typst, como texto, no lugar dos
 * marcadores {@code tailorly:*}. O template continua controlando o layout: o que ele reaproveita
 * em vários pontos (densidade, tamanho de fonte, título do documento, a função {@code lines()})
 * continua em variável; o resto é escrito literalmente, então quem edita encontra o texto onde ele
 * aparece no currículo.
 */
public final class TypstTemplateMaterializer {
  private static final String COMPACT_MARKER = "// tailorly:compact";
  private static final String TITLE_MARKER = "// tailorly:title";
  private static final String FONT_SIZE_MARKER = "// tailorly:font-size";
  private static final String CONTENT_MARKER = "// tailorly:content";
  private static final String MARKUP_SPECIALS = "\\*_`#<>@$[]";

  private TypstTemplateMaterializer() {
  }

  public static String materialize(String source, CvDocument document) {
    if (!source.contains(CONTENT_MARKER)) {
      return source;
    }
    boolean compact = "compact".equals(document.layout().density());
    List<String> lines = new ArrayList<>(List.of(source.split("\n", -1)));
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.contains(COMPACT_MARKER)) {
        lines.set(i, "#let compact = " + compact);
      } else if (line.contains(TITLE_MARKER)) {
        String title = present(document.name());
        lines.set(i, "#set document(title: " + quote(title != null ? title : "Currículo") + ")");
      } else if (line.contains(FONT_SIZE_MARKER)) {
        String withoutMarker = line.replace(FONT_SIZE_MARKER, "").stripTrailing();
        lines.set(i, withoutMarker.replaceFirst("size:\\s*[0-9.]+pt", "size: " + document.layout().fontSize() + "pt"));
      } else if (line.contains(CONTENT_MARKER)) {
        List<String> body = renderBody(document, compact);
        lines.remove(i);
        lines.addAll(i, body);
        i += body.size() - 1;
      }
    }
    return String.join("\n", lines);
  }

  private static List<String> renderBody(CvDocument document, boolean compact) {
    List<String> out = new ArrayList<>();
    String name = present(document.name());
    if (name != null) {
      out.add("#align(center, text(size: 2em, weight: \"bold\")[" + escapeMarkup(name) + "])");
    }
    String headline = present(document.headline());
    if (headline != null) {
      out.add("#align(center, text(size: 1.1em)[" + escapeMarkup(headline) + "])");
    }
    if (!document.contacts().isEmpty()) {
      String joined = document.contacts().stream().map(TypstTemplateMaterializer::renderContact)
          .collect(Collectors.joining(" · "));
      out.add("#align(center, text(size: 0.92em)[" + joined + "])");
    }
    String summary = present(document.summary());
    if (summary != null) {
      out.add("");
      out.add("== Resumo");
      out.add("#lines(" + quote(summary) + ")");
    }
    for (CvDocument.Section section : document.sections()) {
      out.add("");
      out.add("== " + escapeMarkup(section.heading()));
      if (section.compact()) {
        for (CvDocument.Entry entry : section.entries()) {
          out.add("- " + renderEntryCompact(entry));
        }
      } else {
        for (CvDocument.Entry entry : section.entries()) {
          out.addAll(renderEntryBlock(entry, compact));
        }
      }
    }
    return out;
  }

  private static String renderContact(CvDocument.Contact contact) {
    String text = escapeMarkup(contact.text());
    String url = present(contact.url());
    return url != null ? "#link(" + quote(url) + ")[" + text + "]" : text;
  }

  private static String renderEntryCompact(CvDocument.Entry entry) {
    String subtitle = present(entry.subtitle());
    String title = escapeMarkup(entry.title());
    return subtitle == null ? title : title + " — " + escapeMarkup(subtitle);
  }

  private static List<String> renderEntryBlock(CvDocument.Entry entry, boolean compact) {
    List<String> out = new ArrayList<>();
    out.add("#block(breakable: false, below: " + (compact ? "0.6em" : "0.9em") + ")[");
    StringBuilder head = new StringBuilder("  *").append(escapeMarkup(entry.title())).append('*');
    String subtitle = present(entry.subtitle());
    if (subtitle != null) {
      head.append(" — ").append(escapeMarkup(subtitle));
    }
    String period = present(entry.period());
    if (period != null) {
      head.append(" #h(1fr) #text(size: 0.92em)[").append(escapeMarkup(period)).append(']');
    }
    out.add(head.append(" \\").toString());
    String meta = present(entry.meta());
    if (meta != null) {
      out.add("  #text(size: 0.92em, style: \"italic\")[" + escapeMarkup(meta) + "] \\");
    }
    for (String detail : entry.details()) {
      String value = present(detail);
      if (value != null) {
        out.add("  #lines(" + quote(value) + ") \\");
      }
    }
    String link = present(entry.link());
    if (link != null) {
      out.add("  #text(size: 0.92em, link(" + quote(link) + ")[" + escapeMarkup(link) + "])");
    }
    out.add("]");
    return out;
  }

  private static String present(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /** Escapa os caracteres com significado sintático em modo markup do Typst. */
  private static String escapeMarkup(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (MARKUP_SPECIALS.indexOf(c) >= 0) {
        out.append('\\');
      }
      out.append(c);
    }
    return out.toString();
  }

  private static String quote(String value) {
    return '"' + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + '"';
  }
}
