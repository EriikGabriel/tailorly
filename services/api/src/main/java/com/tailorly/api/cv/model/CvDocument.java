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

  /** Uma seção; {@code compact} lista as entradas em uma linha cada (idiomas, tecnologias). */
  public record Section(String key, String heading, boolean compact, List<Entry> entries) {
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
  }
}
