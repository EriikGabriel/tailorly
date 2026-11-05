package com.tailorly.api.cv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CvFilenamesTest {

  @Test
  void joinsFirstNameRoleAndCompany() {
    assertEquals("erik_desenvolvedor-backend_acme.pdf",
        CvFilenames.build("Erik Gabriel Souza", "Desenvolvedor Backend", "Acme", "pdf"));
  }

  @Test
  void stripsAccentsAndSpecialCharacters() {
    assertEquals("jose_analista-de-dados_fintech-solucoes.md",
        CvFilenames.build("José da Silva", "Analista de Dados", "FinTech Soluções!", "md"));
  }

  @Test
  void dropsAdsDecorationFromTheTitle() {
    assertEquals("ana_engenheira-de-software_acme.pdf",
        CvFilenames.build("Ana", "Engenheira de Software (Remoto) - São Paulo", "Acme", "pdf"));
    assertEquals("ana_engenheira-de-software_acme.pdf",
        CvFilenames.build("Ana", "Engenheira de Software | Pleno", "Acme", "pdf"));
  }

  @Test
  void omitsMissingPartsAndFallsBackToGenericName() {
    assertEquals("erik_acme.pdf", CvFilenames.build("Erik", null, "Acme", "pdf"));
    assertEquals("desenvolvedor_acme.pdf", CvFilenames.build(" ", "Desenvolvedor", "Acme", "pdf"));
    assertEquals("curriculo.pdf", CvFilenames.build(null, "", "  ", "pdf"));
  }

  @Test
  void limitsEachPartLength() {
    String name = CvFilenames.build("Erik", "a".repeat(80), "Acme", "pdf");
    assertEquals("erik_" + "a".repeat(30) + "_acme.pdf", name);
  }
}
