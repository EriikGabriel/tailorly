package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class JobSectionClassifierTest {
  private static JobSections classify(String html, JobSection defaultSection) {
    JobSections sections = new JobSections();
    JobSectionClassifier.classify(HtmlText.fromMarkupOrText(html), defaultSection, sections);
    return sections;
  }

  @Test
  void recognizesPortugueseAndEnglishHeadings() {
    assertEquals(JobSection.REQUIREMENTS, JobSectionClassifier.sectionFor("Requisitos e qualificações"));
    assertEquals(JobSection.REQUIREMENTS, JobSectionClassifier.sectionFor("What we're looking for"));
    assertEquals(JobSection.RESPONSIBILITIES, JobSectionClassifier.sectionFor("Principais responsabilidades"));
    assertEquals(JobSection.RESPONSIBILITIES, JobSectionClassifier.sectionFor("O que você vai fazer"));
    assertEquals(JobSection.BENEFITS, JobSectionClassifier.sectionFor("Benefícios"));
    assertEquals(JobSection.NICE_TO_HAVE, JobSectionClassifier.sectionFor("Diferenciais"));
    assertEquals(JobSection.HIRING_PROCESS, JobSectionClassifier.sectionFor("Etapas do processo seletivo"));
    assertNull(JobSectionClassifier.sectionFor("Conhecimento em Docker"));
  }

  @Test
  void desiredRequirementsAreNiceToHaveAndBenefitsBeatDifferentials() {
    assertEquals(JobSection.NICE_TO_HAVE, JobSectionClassifier.sectionFor("Requisitos desejáveis"));
    assertEquals(JobSection.BENEFITS, JobSectionClassifier.sectionFor("Benefícios e diferenciais"));
  }

  @Test
  void headingWithoutColonSwitchesSectionOnlyWhenItLooksLikeAHeading() {
    JobSections sections = classify(
        "<h3>Requisitos</h3><ul><li>Java</li><li>Atividades de CI/CD em pipelines longos de entrega</li></ul>",
        JobSection.OTHER);

    assertEquals(List.of("Java", "Atividades de CI/CD em pipelines longos de entrega"),
        sections.get(JobSection.REQUIREMENTS));
    assertEquals(List.of(), sections.get(JobSection.RESPONSIBILITIES));
  }

  @Test
  void plainShortHeadingsAreRecognizedInTextWithoutMarkup() {
    JobSections sections = classify("Sobre a vaga\nTime de dados\nRequisitos\nSQL\nBenefícios\nVR", JobSection.OTHER);

    assertEquals(List.of("Sobre a vaga", "Time de dados"), sections.get(JobSection.OTHER));
    assertEquals(List.of("SQL"), sections.get(JobSection.REQUIREMENTS));
    assertEquals(List.of("VR"), sections.get(JobSection.BENEFITS));
  }

  @Test
  void unknownRealHeadingEndsTheCurrentSection() {
    JobSections sections = classify(
        "<p>Benefícios:</p><ul><li>VR</li></ul><h2>Sobre o nosso time de engenharia</h2><p>Texto institucional</p>",
        JobSection.OTHER);

    assertEquals(List.of("VR"), sections.get(JobSection.BENEFITS));
    assertEquals(List.of("Texto institucional"), sections.get(JobSection.OTHER));
  }

  @Test
  void defaultSectionAppliesUntilTheFirstHeading() {
    JobSections sections = classify("Java<br>Spring<br>Diferenciais:<br>Kafka", JobSection.REQUIREMENTS);

    assertEquals(List.of("Java", "Spring"), sections.get(JobSection.REQUIREMENTS));
    assertEquals(List.of("Kafka"), sections.get(JobSection.NICE_TO_HAVE));
  }

  @Test
  void duplicatedLinesAppearOncePerSection() {
    JobSections sections = classify("<p>Igual</p><p>Igual</p>", JobSection.OTHER);

    assertEquals(List.of("Igual"), sections.get(JobSection.OTHER));
  }
}
