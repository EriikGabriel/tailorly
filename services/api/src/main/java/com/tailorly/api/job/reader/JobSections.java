package com.tailorly.api.job.reader;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Linhas de uma vaga agrupadas por seção, sem repetições exatas dentro de cada seção. */
final class JobSections {
  private final Map<JobSection, Set<String>> items = new EnumMap<>(JobSection.class);

  void add(JobSection section, String text) {
    items.computeIfAbsent(section, ignored -> new LinkedHashSet<>()).add(text);
  }

  List<String> get(JobSection section) {
    return new ArrayList<>(items.getOrDefault(section, Set.of()));
  }
}
