package com.tailorly.api.cv.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CvPlanTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static CvDocument document() {
    return new CvDocument("Ana", null, null, List.of(), List.of(
        new CvDocument.Section("experience", "Experiência profissional", false, List.of(
            new CvDocument.Entry("experience:0", "A", null, null, null, List.of(), null),
            new CvDocument.Entry("experience:1", "B", null, null, null, List.of(), null))),
        new CvDocument.Section("projects", "Projetos", false, List.of(
            new CvDocument.Entry("projects:0", "P0", null, null, null, List.of(), null),
            new CvDocument.Entry("projects:1", "P1", null, null, null, List.of(), null)))));
  }

  @Test
  @DisplayName("um plano da IA só mantém seções e itens que existem e não reordena a cronologia")
  void parseDropsUnknownReferences() throws Exception {
    var plan = CvPlan.parse(MAPPER.readTree("""
        {"sectionOrder":["projects","inventada","experience","projects"],
         "entryOrder":{"projects":["projects:1","novo","projects:0"],"experience":["experience:1","experience:0"],
                       "inventada":["x"]},
         "density":"gigante"}"""), document());

    assertEquals(List.of("projects", "experience"), plan.sectionOrder());
    assertEquals(List.of("projects:1", "projects:0"), plan.entryOrder().get("projects"));
    assertEquals(false, plan.entryOrder().containsKey("experience"));
    assertEquals("normal", plan.density());
  }

  @Test
  @DisplayName("aplicar o plano muda ordem e densidade, nunca o texto")
  void applyReordersWithoutChangingText() throws Exception {
    var plan = CvPlan.parse(MAPPER.readTree("""
        {"sectionOrder":["projects"],"entryOrder":{"projects":["projects:1"]},"density":"compact"}"""), document());

    CvDocument applied = plan.apply(document());

    assertEquals(List.of("projects", "experience"), applied.sections().stream().map(s -> s.key()).toList());
    assertEquals(List.of("P1", "P0"), applied.sections().getFirst().entries().stream().map(e -> e.title()).toList());
    assertEquals("compact", applied.layout().density());
    assertEquals(document().sections().get(0).entries(), applied.sections().get(1).entries());
    assertEquals("normal", CvPlan.neutral().apply(document()).layout().density());
  }
}
