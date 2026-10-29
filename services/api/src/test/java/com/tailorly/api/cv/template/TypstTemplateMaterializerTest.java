package com.tailorly.api.cv.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.cv.model.CvDocument;
import org.junit.jupiter.api.Test;

class TypstTemplateMaterializerTest {
  private static final String SOURCE = "#let compact = false // tailorly:compact\n"
      + "#set document(title: \"Currículo\") // tailorly:title\n"
      + "#set text(size: 10.5pt, lang: \"pt\") // tailorly:font-size\n"
      + "// tailorly:content\n";

  @Test
  void writesApprovedTextDirectlyInTheBody() {
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

    String result = TypstTemplateMaterializer.materialize(SOURCE, document);

    assertFalse(result.contains("tailorly:"));
    // O nome entra literal no corpo (modo markup, sem aspas) e escapado como string no título.
    assertTrue(result.contains("Ana \"Santos\"])"));
    assertTrue(result.contains("#set document(title: \"Ana \\\"Santos\\\"\")"));
    // Itens de seção compacta viram texto puro, não um dicionário com chaves.
    assertTrue(result.contains("- Inglês — Avançado"));
  }

  @Test
  void escapesTypstSyntaxCharactersInPlainText() {
    CvDocument document = new CvDocument(
        "Dev #hack [x] $y$ <tag> @cite *bold* _em_ `code` \\slash", null, null, List.of(), List.of());

    String result = TypstTemplateMaterializer.materialize(SOURCE, document);

    assertTrue(result.contains(
        "Dev \\#hack \\[x\\] \\$y\\$ \\<tag\\> \\@cite \\*bold\\* \\_em\\_ \\`code\\` \\\\slash"));
  }

  @Test
  void setsDefaultTitleAndSkipsAbsentFields() {
    CvDocument document = new CvDocument(null, null, null, List.of(), List.of());

    String result = TypstTemplateMaterializer.materialize(SOURCE, document);

    assertTrue(result.contains("#set document(title: \"Currículo\")"));
    assertFalse(result.contains("align(center"));
  }

  @Test
  void leavesAlreadyMaterializedTemplatesUnchanged() {
    String result = TypstTemplateMaterializer.materialize("#let cv = (name: \"x\")\n",
        new CvDocument("Outro nome", null, null, List.of(), List.of()));

    assertEquals("#let cv = (name: \"x\")\n", result);
  }
}
