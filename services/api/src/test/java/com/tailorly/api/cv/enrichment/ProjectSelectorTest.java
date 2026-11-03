package com.tailorly.api.cv.enrichment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvLanguage;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.junit.jupiter.api.Test;

class ProjectSelectorTest {
  private static JobTerms terms(String... skills) {
    return JobTerms.of(JobPostingResponse.builder().skills(List.of(skills)).build(), null);
  }

  private static CvDocument.Entry base(int index, String title, String detail) {
    return new CvDocument.Entry("projects:" + index, title, null, null, null, List.of(detail), null);
  }

  private static CvDocument document(CvDocument.Entry... projects) {
    List<CvDocument.Section> sections = projects.length == 0 ? List.of()
        : List.of(new CvDocument.Section("projects", "Projetos", false, List.of(projects)));
    return new CvDocument("Ana", "Dev", null, List.of(), sections);
  }

  private static ConfirmedEvidence github(String repo, String claim, String excerpt) {
    return new ConfirmedEvidence(UUID.randomUUID(), "https://github.com/ana/" + repo, excerpt,
        ConfirmedEvidence.CANDIDATE_PREFIX + repo, "github", repo, claim);
  }

  private static List<String> titles(CvDocument document) {
    return document.sections().stream().filter(section -> section.key().equals("projects")).findFirst()
        .orElseThrow().entries().stream().map(CvDocument.Entry::title).toList();
  }

  @Test
  void withoutConfirmedCandidatesTheDocumentIsUntouched() {
    CvDocument document = document(base(0, "A", "x"));
    ProjectSelector.Selection selection = ProjectSelector.apply(document, List.of(), terms("java"), CvLanguage.PT_BR);
    assertSame(document, selection.document());
    assertTrue(selection.added().isEmpty());
  }

  @Test
  void candidatesCompleteTheMinimumOfThreeProjects() {
    CvDocument document = document(base(0, "Loja", "App em PHP"));
    ProjectSelector.Selection selection = ProjectSelector.apply(document, List.of(
        github("api-java", "API REST em Java", "Java REST"), github("site", "Site estático", "HTML")),
        terms("java"), CvLanguage.PT_BR);

    assertEquals(3, titles(selection.document()).size());
    assertEquals(List.of("api-java", "site"), selection.added());
    assertTrue(selection.omitted().isEmpty());
    // O mais relevante para a vaga vai primeiro; o candidato carrega o link e os detalhes confirmados.
    assertEquals("api-java", titles(selection.document()).getFirst());
    CvDocument.Entry candidate = selection.document().sections().getFirst().entries().getFirst();
    assertEquals("github:api-java", candidate.id());
    assertEquals("https://github.com/ana/api-java", candidate.link());
    assertEquals(List.of("API REST em Java"), candidate.details());
  }

  @Test
  void aMoreRelevantCandidateReplacesTheWeakestBaseProjectWhenFull() {
    CvDocument document = document(base(0, "A", "Java"), base(1, "B", "Java"), base(2, "C", "Java"),
        base(3, "D", "Java"), base(4, "E", "PHP"));
    ProjectSelector.Selection selection = ProjectSelector.apply(document,
        List.of(github("api-go", "API em Go", "Go Docker")), terms("java", "go", "docker"), CvLanguage.PT_BR);

    assertEquals(List.of("api-go"), selection.added());
    assertEquals(List.of("E"), selection.omitted());
    assertEquals(5, titles(selection.document()).size());
  }

  @Test
  void createsTheProjectsSectionInTheDocumentLanguageWhenTheBaseHasNone() {
    CvDocument document = new CvDocument("Ana", "Dev", null, List.of(), List.of(
        new CvDocument.Section("experience", "Professional Experience", false, List.of())));
    ProjectSelector.Selection selection = ProjectSelector.apply(document, List.of(
        github("a", "x", "x"), github("b", "y", "y"), github("c", "z", "z")), terms(), CvLanguage.EN);

    assertEquals(List.of("experience", "projects"),
        selection.document().sections().stream().map(CvDocument.Section::key).toList());
    assertEquals("Projects", selection.document().sections().get(1).heading());
  }
}
