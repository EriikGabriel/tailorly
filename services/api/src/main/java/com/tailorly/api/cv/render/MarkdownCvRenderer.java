package com.tailorly.api.cv.render;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvDocument.Contact;
import com.tailorly.api.cv.model.CvDocument.Entry;
import com.tailorly.api.cv.model.CvDocument.Section;
import org.springframework.stereotype.Component;

/** Gera o Markdown do currículo a partir do mesmo {@link CvDocument} usado pelo PDF. */
@Component
public class MarkdownCvRenderer {
  private static final Pattern SPECIAL = Pattern.compile("([\\\\`*_\\[\\]<>])");
  private static final Pattern LINE_START = Pattern.compile("(?m)^(\\s*)([#>+-]|\\d+[.)])(?=\\s|$)");

  public byte[] render(CvDocument document) {
    StringBuilder out = new StringBuilder();
    if (document.name() != null) {
      out.append("# ").append(inline(document.name())).append("\n\n");
    }
    if (document.headline() != null) {
      out.append(inline(document.headline())).append("\n\n");
    }
    if (!document.contacts().isEmpty()) {
      out.append(String.join(" · ", document.contacts().stream().map(MarkdownCvRenderer::contact).toList()))
          .append("\n\n");
    }
    if (document.summary() != null) {
      out.append("## Resumo\n\n").append(block(document.summary())).append("\n\n");
    }
    for (Section section : document.sections()) {
      out.append("## ").append(section.heading()).append("\n\n");
      if (section.grouped()) {
        for (Entry entry : section.entries()) {
          out.append("- **").append(inline(entry.title())).append(":** ")
              .append(inline(String.join(", ", entry.detailLines()))).append('\n');
        }
        out.append('\n');
      } else if (section.compact()) {
        out.append(section.entries().stream().map(MarkdownCvRenderer::compactItem)
            .collect(java.util.stream.Collectors.joining(" · "))).append("\n\n");
      } else {
        section.entries().forEach(entry -> entry(out, entry));
      }
    }
    return (out.toString().stripTrailing() + "\n").getBytes(StandardCharsets.UTF_8);
  }

  private static void entry(StringBuilder out, Entry entry) {
    out.append("### ").append(inline(entry.title()));
    if (entry.subtitle() != null) {
      out.append(" — ").append(inline(entry.subtitle()));
    }
    out.append("\n\n");
    List<String> meta = new ArrayList<>();
    if (entry.period() != null) {
      meta.add(inline(entry.period()));
    }
    if (entry.meta() != null) {
      meta.add(inline(entry.meta()));
    }
    if (!meta.isEmpty()) {
      out.append('*').append(String.join(" · ", meta)).append("*\n\n");
    }
    List<String> details = entry.detailLines();
    for (String detail : details) {
      // Um item que começa com "#" ou "1." viraria título ou lista aninhada dentro do bullet.
      out.append("- ").append(LINE_START.matcher(inline(detail)).replaceAll("$1\\\\$2")).append('\n');
    }
    if (!details.isEmpty()) {
      out.append('\n');
    }
    if (entry.link() != null) {
      out.append('<').append(entry.link().replace(">", "%3E").replace(" ", "%20")).append(">\n\n");
    }
  }

  private static String compactItem(Entry entry) {
    return entry.subtitle() == null ? inline(entry.title())
        : inline(entry.title()) + " (" + inline(entry.subtitle()) + ")";
  }

  private static String contact(Contact contact) {
    String text = inline(contact.text());
    if (contact.url() == null) {
      return text;
    }
    return "[" + text + "](" + contact.url().replace("(", "%28").replace(")", "%29").replace(" ", "%20") + ")";
  }

  /** Texto de uma linha: caracteres de formatação escapados e quebras de linha viram espaço. */
  private static String inline(String text) {
    return escape(Objects.requireNonNull(text).replaceAll("\\s*\\R\\s*", " ").strip());
  }

  /** Texto de vários parágrafos: preserva as quebras de linha e escapa o que viraria formatação. */
  private static String block(String text) {
    String escaped = LINE_START.matcher(escape(text.strip())).replaceAll("$1\\\\$2");
    return escaped.replaceAll("[ \\t]*\\R", "  \n");
  }

  private static String escape(String text) {
    return SPECIAL.matcher(text).replaceAll("\\\\$1");
  }
}
