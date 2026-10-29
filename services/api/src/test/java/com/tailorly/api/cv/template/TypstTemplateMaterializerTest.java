package com.tailorly.api.cv.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.cv.model.CvDocument;
import org.junit.jupiter.api.Test;

class TypstTemplateMaterializerTest {
  @Test
  void putsApprovedTextInTheEditableTemplate() {
    CvDocument document = new CvDocument(
        "Ana \"Santos\"",
        "Engenheira de software",
        "Resumo\ncom duas linhas",
        List.of(new CvDocument.Contact("ana@example.com", null)),
        List.of(new CvDocument.Section(
            "languages",
            "Idiomas",
            true,
            List.of(new CvDocument.Entry("Inglês", "Avançado", null, null, List.of(), null)))));

    String source = TypstTemplateMaterializer.materialize(
        "#let cv = json(\"data.json\")\n#cv.name\n", document);

    assertFalse(source.contains("json(\"data.json\")"));
    assertTrue(source.contains("\\\"Santos\\\""));
    assertTrue(source.contains("\"Inglês\""));
    assertTrue(source.contains("\"Avançado\""));
  }
}
