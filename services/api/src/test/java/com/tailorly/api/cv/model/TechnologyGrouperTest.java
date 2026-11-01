package com.tailorly.api.cv.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TechnologyGrouperTest {
  private static CvDocument document(String... names) {
    List<CvDocument.Entry> entries = new java.util.ArrayList<>();
    for (int i = 0; i < names.length; i++) {
      entries.add(new CvDocument.Entry("technologies:" + i, names[i], null, null, null, List.of(), null));
    }
    return new CvDocument(null, null, null, List.of(),
        List.of(new CvDocument.Section("technologies", "Tecnologias", true, entries)));
  }

  @Test
  void groupsKnownTechnologiesAndKeepsUnknownOnesUnderOthers() {
    CvDocument grouped = TechnologyGrouper.apply(document("Java", "Spring Boot", "Python", "Flask", "React",
        "TypeScript", "PostgreSQL", "MySQL", "Docker", "Git", "Foobar"));

    CvDocument.Section section = grouped.sections().getFirst();
    assertTrue(section.grouped());
    List<String> lines = section.entries().stream()
        .map(entry -> entry.title() + ": " + String.join(", ", entry.detailLines())).toList();
    assertEquals(List.of(
        "Linguagens: Java, Python, TypeScript",
        "Backend: Spring Boot, Flask",
        "Frontend: React",
        "Banco de dados: PostgreSQL, MySQL",
        "Cloud e DevOps: Docker",
        "Ferramentas: Git",
        "Outras: Foobar"), lines);
  }

  @Test
  void keepsTheFlatListWhenGroupingDoesNotSaveSpace() {
    CvDocument flat = document("Java", "React", "Docker", "Git", "MySQL");

    assertSame(flat, TechnologyGrouper.apply(flat));
    CvDocument tiny = document("Java", "Spring Boot");
    assertSame(tiny, TechnologyGrouper.apply(tiny));
  }
}
