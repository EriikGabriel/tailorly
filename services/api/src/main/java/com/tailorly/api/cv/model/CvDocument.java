package com.tailorly.api.cv.model;

import java.util.List;

/**
 * Conteúdo de um currículo pronto para renderização. Todo texto vem de valores aprovados do
 * snapshot do Ground Truth; os renderizadores só dispõem esse conteúdo e nunca acrescentam fatos.
 * Campos ausentes ficam nulos ou vazios e simplesmente não são exibidos.
 */
public record CvDocument(
    String name,
    String headline,
    String summary,
    List<Contact> contacts,
    List<Section> sections,
    Layout layout
) {
  public static final Layout DEFAULT_LAYOUT = new Layout("normal", 10.5);

  public CvDocument(String name, String headline, String summary, List<Contact> contacts,
      List<Section> sections) {
    this(name, headline, summary, contacts, sections, DEFAULT_LAYOUT);
  }

  /** Parâmetros de apresentação que o template lê; {@code density}: {@code normal} ou {@code compact}. */
  public record Layout(String density, double fontSize) {
  }

  /** Um meio de contato; {@code url} é preenchida quando o valor pode virar um link clicável. */
  public record Contact(String text, String url) {
  }

  /**
   * Uma seção. {@code compact} junta as entradas numa linha corrida (idiomas, tecnologias soltas);
   * {@code grouped} indica entradas que são grupos (título = rótulo do grupo, primeiro detalhe = itens
   * separados por vírgula), exibidas como uma linha "Rótulo: itens" por grupo.
   */
  public record Section(String key, String heading, boolean compact, boolean grouped, List<Entry> entries) {
    public Section(String key, String heading, boolean compact, List<Entry> entries) {
      this(key, heading, compact, false, entries);
    }

    public Section withEntries(List<Entry> newEntries) {
      return new Section(key, heading, compact, grouped, newEntries);
    }
  }

  /** {@code id} identifica o item dentro do documento (por exemplo {@code projects:2}) para reordenação. */
  public record Entry(
      String id,
      String title,
      String subtitle,
      String meta,
      String period,
      List<String> details,
      String link
  ) {
    public Entry(String title, String subtitle, String meta, String period, List<String> details, String link) {
      this(null, title, subtitle, meta, period, details, link);
    }

    public Entry withId(String newId) {
      return new Entry(newId, title, subtitle, meta, period, details, link);
    }

    /**
     * Os detalhes quebrados em itens de lista: um por linha, sem marcador de lista que o texto de origem
     * já trouxesse ({@code -}, {@code •}, {@code *}), para os renderizadores desenharem os próprios bullets.
     */
    public List<String> detailLines() {
      List<String> lines = new java.util.ArrayList<>();
      for (String detail : details) {
        if (detail == null) {
          continue;
        }
        for (String line : detail.split("\\R")) {
          String item = line.strip().replaceFirst("^[-•*–]\\s+", "").strip();
          if (!item.isEmpty()) {
            lines.add(item);
          }
        }
      }
      return List.copyOf(lines);
    }
  }
}
