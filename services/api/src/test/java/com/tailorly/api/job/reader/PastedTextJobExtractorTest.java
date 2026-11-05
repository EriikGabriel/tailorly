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
  private static final String STUDIO_RX = """
      Quem somos:

      A Studio Rx é um hub de produção da Omnicom Production, criada para apoiar as necessidades das agências do grupo, tendo como pilares principais a qualidade e eficiência.


      O que buscamos:

      Estamos em busca de um(a) Desenvolvedor(a) Júnior com interesse em desenvolvimento front-end, cujo foco principal será atuar em projetos de baixa complexidade relacionados à criação e manutenção de banners HTML5, peças digitais e e-mails marketing.

      Requisitos:
      Conhecimento em HTML5, CSS3 e JavaScript básico;
      Conhecimento básico em Git ou outros sistemas de controle de versão;
      Inglês intermediário para leitura, interpretação de documentação e comunicação escrita

      Modelo de contratação: CLT
      Modelo de trabalho: 100% Remoto
      """;

  @Test
  void findsTitleAndCompanyInAProsePosting() {
    var job = extractor.extract(STUDIO_RX, null, null);

    assertEquals("Desenvolvedor(a) Júnior", job.title());
    assertEquals("Studio Rx", job.company().name());
  }

  @Test
  void readsLabelledTitleAndCompany() {
    var job = extractor.extract("Cargo: Analista de Dados Pleno\nEmpresa: Acme Ltda\nRequisitos\nSQL", null, null);

    assertEquals("Analista de Dados Pleno", job.title());
    assertEquals("Acme Ltda", job.company().name());
  }

  @Test
  void readsCompanyFromAnIntroductionSentence() {
    var job = extractor.extract("Somos a Acme, líder em logística.\nBuscamos Engenheiro de Dados com 3 anos.", null, null);

    assertEquals("Engenheiro de Dados", job.title());
    assertEquals("Acme", job.company().name());
  }

  @Test
  void genericWordsAreNotTakenAsTitleOrCompany() {
    var job = extractor.extract("A vaga é para quem gosta de desafios.\nEstamos contratando uma pessoa para o time de dados.", null, null);

    assertNull(job.title());
    assertNull(job.company());
  }

  @Test
  void givenValuesStillWin() {
    var job = extractor.extract(STUDIO_RX, "Meu cargo", "Minha empresa");

    assertEquals("Meu cargo", job.title());
    assertEquals("Minha empresa", job.company().name());
  }
}
