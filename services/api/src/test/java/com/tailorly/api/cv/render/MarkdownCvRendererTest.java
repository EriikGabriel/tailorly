package com.tailorly.api.cv.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.tailorly.api.cv.model.CvDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarkdownCvRendererTest {
  private final MarkdownCvRenderer renderer = new MarkdownCvRenderer();

  private String render(CvDocument document) {
    return new String(renderer.render(document), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("renderiza cabeçalho, contatos, resumo e seções")
  void rendersSections() {
    String markdown = render(new CvDocument("Ana Souza", "Engenheira", "Resumo da Ana.",
        List.of(new CvDocument.Contact("ana@example.com", "mailto:ana@example.com"),
            new CvDocument.Contact("São Paulo", null)),
        List.of(
            new CvDocument.Section("experience", "Experiência profissional", false, List.of(
                new CvDocument.Entry("Dev", "Acme", "CLT", "05/2021 – Atual", List.of("Fez coisas."), null))),
            new CvDocument.Section("languages", "Idiomas", true, List.of(
                new CvDocument.Entry("Inglês", "Avançado", null, null, List.of(), null))))));

    assertEquals("""
        # Ana Souza

        Engenheira

        [ana@example.com](mailto:ana@example.com) · São Paulo

        ## Resumo

        Resumo da Ana.

        ## Experiência profissional

        ### Dev — Acme

        *05/2021 – Atual · CLT*

        - Fez coisas.

        ## Idiomas

        Inglês (Avançado)
        """, markdown);
  }

  @Test
  @DisplayName("separa métricas em bullets e agrupa tecnologias em uma linha por grupo")
  void rendersBulletsAndGroups() {
    String markdown = render(new CvDocument(null, null, null, List.of(), List.of(
        new CvDocument.Section("technologies", "Tecnologias", false, true, List.of(
            new CvDocument.Entry("Backend", null, null, null, List.of("Java, Spring Boot"), null),
            new CvDocument.Entry("Frontend", null, null, null, List.of("React"), null))),
        new CvDocument.Section("projects", "Projetos", false, List.of(
            new CvDocument.Entry("App", null, null, null, List.of("Reduziu o tempo em 30%\n• Atendeu 2 mil usuários"),
                null))))));

    assertTrue(markdown.contains("- **Backend:** Java, Spring Boot\n- **Frontend:** React\n"));
    assertTrue(markdown.contains("- Reduziu o tempo em 30%\n- Atendeu 2 mil usuários\n"));
  }

  @Test
  @DisplayName("escapa formatação e HTML vindos dos valores")
  void escapesValues() {
    String markdown = render(new CvDocument("A *B*", null, null, List.of(), List.of(
        new CvDocument.Section("projects", "Projetos", false, List.of(
            new CvDocument.Entry("<script>x</script>", "[link](http://evil)", null, null,
                List.of("# título\n- item\nlinha `código`"), "https://example.com/a b"))))));

    assertTrue(markdown.contains("# A \\*B\\*"));
    assertTrue(markdown.contains("\\<script\\>x\\</script\\>"));
    assertTrue(markdown.contains("\\[link\\](http://evil)"));
    assertTrue(markdown.contains("\\# título"));
    assertTrue(markdown.contains("- item\n"));
    assertTrue(markdown.contains("\\`código\\`"));
    assertTrue(markdown.contains("<https://example.com/a%20b>"));
    assertFalse(markdown.contains("<script>"));
  }
}
