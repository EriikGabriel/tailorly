package com.tailorly.api.cv.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.cv.model.CvDocument;
import org.junit.jupiter.api.Test;

class EvidenceSubjectsTest {
  private static CvDocument document() {
    return new CvDocument("Ana", null, null, List.of(), List.of(
        new CvDocument.Section("experience", "Experiência", false, List.of(
            new CvDocument.Entry("experience:0", "Engenheira", "Acme Corp", null, "2021", List.of(), null),
            new CvDocument.Entry("experience:1", "Estagiária", null, null, "2020", List.of(), null))),
        new CvDocument.Section("projects", "Projetos", false, List.of(
            new CvDocument.Entry("projects:0", "UFSCar Planner", "Projeto acadêmico", null, null, List.of(),
                "https://github.com/ana/ufscar-planner"),
            new CvDocument.Entry("projects:1", "Sem link", null, null, null, List.of(), null)))));
  }

  @Test
  void projectsWithAndWithoutLinkAndExperiencesWithACompanyBecomeSubjects() {
    List<EvidenceSubject> subjects = EvidenceSubjects.of(document());

    assertEquals(List.of("experience:0", "projects:0", "projects:1"),
        subjects.stream().map(EvidenceSubject::key).toList());
  }

  @Test
  void projectsAreSearchedByNameAndRepositoryExperiencesOnlyByCompanyInNotes() {
    List<EvidenceSubject> subjects = EvidenceSubjects.of(document());

    EvidenceSubject experience = subjects.get(0);
    assertEquals(List.of("Acme Corp"), experience.aliases());
    assertFalse(experience.searchable());

    EvidenceSubject project = subjects.get(1);
    assertEquals(List.of("UFSCar Planner", "ufscar-planner"), project.aliases());
    assertTrue(project.searchable());
    assertEquals("UFSCar Planner Projeto acadêmico", project.searchQuery());
    assertTrue(subjects.get(2).searchable(), "um projeto sem link também é procurado");
  }
}
