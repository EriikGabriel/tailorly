package com.tailorly.api.job.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class PastedTextJobExtractorTest {
  private final PastedTextJobExtractor extractor = new PastedTextJobExtractor();

  @Test
  void aSectionHeadingIsNeverTheTitle() {
    var job = extractor.extract("Requisitos e qualificações\nExperiência com Java;\nConhecimento em TDD;", null, null);

    assertNull(job.title());
    assertEquals(List.of("Experiência com Java;", "Conhecimento em TDD;"), job.requirements());
  }

  @Test
  void aFirstLineThatLooksLikeATitleIsUsedAndAGivenTitleWins() {
    assertEquals("Engenheiro Backend", extractor.extract("Engenheiro Backend\nRequisitos\nJava", null, null).title());
    assertEquals("Meu título", extractor.extract("Engenheiro Backend\nRequisitos\nJava", "Meu título", null).title());
    assertNull(extractor.extract("Estamos contratando uma pessoa para o time de dados.", null, null).title());
  }
}
