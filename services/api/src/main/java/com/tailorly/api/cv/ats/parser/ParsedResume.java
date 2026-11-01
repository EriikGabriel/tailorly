package com.tailorly.api.cv.ats.parser;

import java.util.List;
import java.util.Set;

/**
 * O que um parser de triagem consegue tirar do texto de um PDF: nome provável, contatos, seções
 * reconhecidas pelo título e as entradas datadas (cargos, formação). É a visão "do outro lado" do
 * currículo, usada para conferir se o que escrevemos sobrevive à leitura automática.
 */
public record ParsedResume(
    String text,
    int pages,
    String name,
    List<String> emails,
    List<String> phones,
    List<String> links,
    List<Section> sections,
    Set<String> skills
) {
  /** {@code key} é a seção canônica ({@code experience}, {@code education}...), nula se o título não foi reconhecido. */
  public record Section(String heading, String key, List<String> lines, List<Entry> entries) {
  }

  /** Uma entrada que começa numa linha com período: o texto antes da data e o período lido. */
  public record Entry(String head, String period, List<String> body) {
  }

  public List<Entry> entriesOf(String key) {
    return sections.stream().filter(section -> key.equals(section.key()))
        .flatMap(section -> section.entries().stream()).toList();
  }

  public boolean hasSection(String key) {
    return sections.stream().anyMatch(section -> key.equals(section.key()));
  }
}
