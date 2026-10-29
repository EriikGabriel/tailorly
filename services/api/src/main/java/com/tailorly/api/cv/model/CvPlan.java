package com.tailorly.api.cv.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Decisões de apresentação da esteira: ordem das seções, ordem dos itens de listas sem cronologia
 * e densidade do layout. É a única coisa que a IA pode escolher; um plano só referencia seções e
 * itens que já existem no documento, então nunca introduz texto.
 *
 * <p>Experiência e formação não são reordenadas por item: a ordem cronológica do Currículo Base
 * é a que os sistemas de triagem esperam.</p>
 */
public record CvPlan(List<String> sectionOrder, Map<String, List<String>> entryOrder, String density) {
  public static final Set<String> DENSITIES = Set.of("normal", "compact");
  private static final Set<String> REORDERABLE =
      Set.of("technologies", "projects", "certifications", "volunteering", "languages");

  public static CvPlan neutral() {
    return new CvPlan(List.of(), Map.of(), "normal");
  }

  public CvPlan withDensity(String newDensity) {
    return new CvPlan(sectionOrder, entryOrder, newDensity);
  }

  /** Lê um plano proposto pela IA, descartando tudo o que não corresponde ao documento. */
  public static CvPlan parse(JsonNode root, CvDocument document) {
    List<String> sections = new ArrayList<>();
    for (JsonNode node : root.path("sectionOrder")) {
      String key = node.asText("");
      if (document.sections().stream().anyMatch(section -> section.key().equals(key)) && !sections.contains(key)) {
        sections.add(key);
      }
    }
    Map<String, List<String>> entries = new LinkedHashMap<>();
    for (CvDocument.Section section : document.sections()) {
      if (!REORDERABLE.contains(section.key())) {
        continue;
      }
      List<String> ids = new ArrayList<>();
      for (JsonNode node : root.path("entryOrder").path(section.key())) {
        String id = node.asText("");
        if (section.entries().stream().anyMatch(entry -> id.equals(entry.id())) && !ids.contains(id)) {
          ids.add(id);
        }
      }
      if (!ids.isEmpty()) {
        entries.put(section.key(), ids);
      }
    }
    String density = root.path("density").asText("normal");
    return new CvPlan(sections, entries, DENSITIES.contains(density) ? density : "normal");
  }

  public CvDocument apply(CvDocument document) {
    Map<String, CvDocument.Section> byKey = new LinkedHashMap<>();
    for (CvDocument.Section section : document.sections()) {
      byKey.put(section.key(), reorderEntries(section));
    }
    List<CvDocument.Section> ordered = new ArrayList<>();
    for (String key : sectionOrder) {
      CvDocument.Section section = byKey.remove(key);
      if (section != null) {
        ordered.add(section);
      }
    }
    // O que o plano não mencionou segue no fim, na ordem original.
    ordered.addAll(byKey.values());
    boolean compact = "compact".equals(density);
    return new CvDocument(document.name(), document.headline(), document.summary(), document.contacts(),
        List.copyOf(ordered), new CvDocument.Layout(compact ? "compact" : "normal", compact ? 10.0 : 10.5));
  }

  private CvDocument.Section reorderEntries(CvDocument.Section section) {
    List<String> ids = entryOrder.get(section.key());
    if (ids == null || !REORDERABLE.contains(section.key())) {
      return section;
    }
    Map<String, CvDocument.Entry> byId = new HashMap<>();
    section.entries().forEach(entry -> byId.put(entry.id(), entry));
    List<CvDocument.Entry> ordered = new ArrayList<>();
    for (String id : ids) {
      CvDocument.Entry entry = byId.remove(id);
      if (entry != null) {
        ordered.add(entry);
      }
    }
    section.entries().stream().filter(entry -> byId.containsKey(entry.id())).forEach(ordered::add);
    return new CvDocument.Section(section.key(), section.heading(), section.compact(), List.copyOf(ordered));
  }
}
