package com.tailorly.api.cv.ats.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResumeParserTest {
  private final ResumeParser parser = new ResumeParser();

  private static final String TYPST_LIKE = """
      Ana Souza
      Engenheira de software
      São Paulo, SP · ana@example.com · (11) 98765-4321 · github.com/ana
      Resumo
      Engenheira com foco em backend.
      Experiência profissional
      Engenheira — Acme 05/2021 – Atual
      • Serviços em Java e Spring em 2023
      • Reduziu o tempo em 30%
      Dev Júnior — Beta 2019 – 2021
      • APIs REST
      Formação acadêmica
      Ciência da Computação — USP 2016 – 2020
      Tecnologias
      Backend: Java, Spring Boot.
      Banco de dados: PostgreSQL, MySQL.
      """;

  @Test
  void readsHeaderSectionsEntriesAndSkills() {
    ParsedResume resume = parser.parseText(TYPST_LIKE, 1);

    assertEquals("Ana Souza", resume.name());
    assertEquals(List.of("ana@example.com"), resume.emails());
    assertEquals(1, resume.phones().size());
    assertTrue(resume.hasSection("experience") && resume.hasSection("education") && resume.hasSection("skills"));

    var jobs = resume.entriesOf("experience");
    assertEquals(2, jobs.size());
    assertEquals("Engenheira — Acme", jobs.get(0).head());
    assertEquals("05/2021 – Atual", jobs.get(0).period());
    // Uma data dentro de uma frase com bullet não abre entrada.
    assertEquals(2, jobs.get(0).body().size());
    assertTrue(resume.skills().containsAll(List.of("Java", "Spring Boot", "PostgreSQL", "MySQL")));
  }

  @Test
  void periodOnItsOwnLineTakesTheTwoPreviousLinesAsTheHead() {
    ParsedResume resume = parser.parseText("""
        Ana
        Experiência profissional
        Engenheira
        Acme
        05/2021 – Atual
        Serviços em Java
        """, 1);

    var jobs = resume.entriesOf("experience");
    assertEquals(1, jobs.size());
    assertEquals("Engenheira Acme", jobs.get(0).head());
    assertEquals(List.of("Serviços em Java"), jobs.get(0).body());
  }

  @Test
  void creativeHeadingsAreNotRecognized() {
    ParsedResume resume = parser.parseText("Ana\nMinha trajetória\nEngenheira — Acme 2020 – 2021\n", 1);

    assertTrue(resume.sections().isEmpty());
    assertNull(resume.sections().stream().map(ParsedResume.Section::key).findFirst().orElse(null));
  }

  @Test
  void unreadablePdfYieldsNothing() {
    assertTrue(parser.parse("não é pdf".getBytes()).isEmpty());
    assertTrue(parser.parse(null).isEmpty());
  }
}
