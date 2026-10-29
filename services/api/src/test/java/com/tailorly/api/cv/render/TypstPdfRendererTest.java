package com.tailorly.api.cv.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.template.CvTemplateCatalog;
import com.tailorly.api.cv.template.TypstTemplateMaterializer;
import com.tailorly.api.shared.exception.ServiceUnavailableException;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

class TypstPdfRendererTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final CvDocument DOCUMENT = new CvDocument("Ana #let x = 1", "Eng", "Resumo", List.of(
      new CvDocument.Contact("ana@example.com", "mailto:ana@example.com")), List.of());

  private Path script(Path directory, String body) throws IOException {
    Path script = directory.resolve("fake-typst.sh");
    Files.writeString(script, "#!/bin/sh\n" + body);
    script.toFile().setExecutable(true);
    return script;
  }

  @Test
  @DisplayName("entrega o template e o conteúdo como arquivos separados e devolve o PDF compilado")
  void passesContentAsDataFile(@TempDir Path directory) throws IOException {
    Path script = script(directory, """
        if [ "$1" = "--version" ]; then echo "typst 9.9.9"; exit 0; fi
        [ "$1" = "compile" ] && [ -f main.typ ] && [ -f data.json ] || exit 3
        cp main.typ out.pdf && cat data.json >> out.pdf
        """);
    TypstPdfRenderer renderer = new TypstPdfRenderer(script.toString(), MAPPER);

    byte[] pdf = renderer.render("TEMPLATE\n", DOCUMENT);

    String text = new String(pdf, StandardCharsets.UTF_8);
    assertTrue(text.startsWith("TEMPLATE\n"));
    // O conteúdo viaja só em JSON; nada dele é interpolado no código do template.
    assertEquals(DOCUMENT, MAPPER.readValue(text.substring("TEMPLATE\n".length()), CvDocument.class));
    assertTrue(renderer.fingerprint().endsWith("typst 9.9.9"));
  }

  @Test
  @DisplayName("falha de compilação devolve os diagnósticos e não deixa arquivos temporários")
  void compilationFailure(@TempDir Path directory) throws IOException {
    Path script = script(directory, "echo 'erro com Ana' >&2; exit 1\n");
    TypstPdfRenderer renderer = new TypstPdfRenderer(script.toString(), MAPPER);

    UnprocessableRequestException failure = assertThrows(UnprocessableRequestException.class,
        () -> renderer.render("x", DOCUMENT));

    assertEquals("error.cv_generation.template_invalid", failure.getMessage());
    // Os diagnósticos do compilador voltam a quem escreveu o template, para o editor mostrá-los.
    assertEquals(List.of("erro com Ana"), failure.getDetails());
    try (Stream<Path> leftovers = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      assertEquals(0, leftovers.filter(path -> path.getFileName().toString().startsWith("tailorly-cv-")).count());
    }
  }

  @Test
  @DisplayName("sem o executável do Typst a geração fica indisponível")
  void missingExecutable() {
    TypstPdfRenderer renderer = new TypstPdfRenderer("/nonexistent/typst", MAPPER);

    assertThrows(ServiceUnavailableException.class, () -> renderer.render("x", DOCUMENT));
  }

  static boolean typstInstalled() {
    try {
      Process process = new ProcessBuilder("typst", "--version").redirectErrorStream(true).start();
      process.getInputStream().readAllBytes();
      return process.waitFor() == 0;
    } catch (IOException | InterruptedException exception) {
      return false;
    }
  }

  @Test
  @EnabledIf("typstInstalled")
  @DisplayName("compila o template classic com o Typst real")
  void compilesBundledTemplate() throws IOException {
    CvDocument document = new CvDocument("Ana Souza", "Engenheira", "Linha 1\nLinha 2", List.of(
        new CvDocument.Contact("ana@example.com", "mailto:ana@example.com"),
        new CvDocument.Contact("São Paulo", null)), List.of(
        new CvDocument.Section("experience", "Experiência profissional", false, List.of(
            new CvDocument.Entry("Dev #[x] $ \\", "Acme", "CLT", "05/2021 – Atual", List.of("Fez *coisas*.\nOutra linha"),
                "https://example.com"),
            new CvDocument.Entry("Só título", null, null, null, List.of(), null))),
        new CvDocument.Section("languages", "Idiomas", true, List.of(
            new CvDocument.Entry("Inglês", "Avançado", null, null, List.of(), null)))));
    String template = new String(CvTemplateCatalog.class.getClassLoader()
        .getResourceAsStream("cv-templates/classic-v1.typ").readAllBytes(), StandardCharsets.UTF_8);
    // O conteúdo aprovado agora entra como texto literal no corpo, então materializa antes de compilar.
    String materialized = TypstTemplateMaterializer.materialize(template, document);

    byte[] pdf = new TypstPdfRenderer("typst", MAPPER).render(materialized, document);

    assertArrayEquals("%PDF-".getBytes(StandardCharsets.US_ASCII), java.util.Arrays.copyOf(pdf, 5));
  }
}
