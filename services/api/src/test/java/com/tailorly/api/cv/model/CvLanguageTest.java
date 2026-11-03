package com.tailorly.api.cv.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CvLanguageTest {
  @Test
  void fixedPreferencesIgnoreTheJob() {
    assertEquals(CvLanguage.PT_BR, CvLanguage.resolve("pt-BR", "We are hiring a backend engineer with the team"));
    assertEquals(CvLanguage.EN, CvLanguage.resolve("en", "Buscamos pessoa desenvolvedora para a nossa equipe"));
  }

  @Test
  void autoDetectsTheJobLanguage() {
    assertEquals(CvLanguage.EN, CvLanguage.resolve("auto",
        "Senior Backend Engineer. You will work with the team on our platform and have experience with Java."));
    assertEquals(CvLanguage.PT_BR, CvLanguage.resolve("auto",
        "Desenvolvedor Backend. Experiência com Java e conhecimento em bancos de dados para a nossa equipe."));
  }

  @Test
  void unknownOrMissingPreferenceAndEmptyJobFallBackToPortuguese() {
    assertEquals(CvLanguage.PT_BR, CvLanguage.resolve(null, ""));
    assertEquals(CvLanguage.PT_BR, CvLanguage.resolve("fr", null));
    assertEquals("auto", CvLanguage.normalizePreference("klingon"));
    assertEquals("en", CvLanguage.normalizePreference("EN"));
  }

  @Test
  void preferencesDefaultToAuto() {
    assertEquals("auto", CvPreferences.defaults().language());
    assertEquals("auto", new CvPreferences(java.util.List.of(), true).language());
    assertEquals("en", new CvPreferences(java.util.List.of(), true, null, "en").language());
  }
}
