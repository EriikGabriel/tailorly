package com.tailorly.api.cv.evidence;

import java.util.List;

/**
 * Um item do currículo que pode ganhar evidências complementares: um projeto ou uma experiência.
 * {@code key} é o mesmo id da entrada no documento ({@code projects:0}, {@code experience:1}), então o
 * enriquecimento casa evidência e entrada sem depender de link. {@code aliases} são os nomes pelos
 * quais o item é citado em páginas e notas (título, repositório, empresa); {@code searchQuery} é a
 * busca web (só para projetos, pois páginas sobre uma empresa não provam o que a pessoa fez nela).
 */
public record EvidenceSubject(String key, Kind kind, String label, List<String> aliases, String link,
    String searchQuery) {
  public enum Kind {
    PROJECT, EXPERIENCE
  }

  public boolean searchable() {
    return kind == Kind.PROJECT && searchQuery != null;
  }
}
