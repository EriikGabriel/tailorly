package com.tailorly.api.cv.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TermAliasesTest {
  @Test
  void differentSpellingsOfTheSameTechnologyMatch() {
    assertTrue(TermAliases.mentions("Frontend: React, TypeScript", "React.js"));
    assertTrue(TermAliases.mentions("Frontend: ReactJS", "React"));
    assertTrue(TermAliases.mentions("Banco: Postgres", "PostgreSQL"));
    assertTrue(TermAliases.mentions("Banco: PostgreSQL", "Postgres"));
    assertTrue(TermAliases.mentions("Spring Boot", "Spring Framework"));
    assertTrue(TermAliases.mentions("Node.js e NodeJS", "Node"));
  }

  @Test
  void matchesWholeWordsOnly() {
    assertFalse(TermAliases.mentions("JavaScript", "Java"));
    assertFalse(TermAliases.mentions("Reactive systems", "React"));
    assertFalse(TermAliases.mentions("Goiânia", "Go"));
  }
}
