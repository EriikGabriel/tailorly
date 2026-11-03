package com.tailorly.api.cv.ats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import tools.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.ats.parser.ResumeParser;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.cv.render.TypstPdfRenderer;
import com.tailorly.api.cv.template.CvTemplateCatalog;
import com.tailorly.api.cv.template.TypstTemplateMaterializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** O parser precisa recuperar o conteúdo dos PDFs que os próprios templates embarcados produzem. */
class AtsEvaluatorTypstTest {
  private static boolean typstInstalled() {
    try {
      return new ProcessBuilder("typst", "--version").redirectErrorStream(true).start().waitFor() == 0;
    } catch (IOException | InterruptedException exception) {
      return false;
    }
  }

  @Test
  @EnabledIf("typstInstalled")
  void parserRecoversEveryBundledTemplateOutput() throws IOException {
    CvDocument document = new CvDocument("Ana Souza", "Engenheira de software", "Resumo de uma linha.",
        List.of(new CvDocument.Contact("ana@example.com", "mailto:ana@example.com"),
            new CvDocument.Contact("(11) 98765-4321", null)),
        List.of(
            new CvDocument.Section("technologies", "Tecnologias", false, true, List.of(
                new CvDocument.Entry("technologies:backend", "Backend", null, null, null,
                    List.of("Java, Spring Boot"), null),
                new CvDocument.Entry("technologies:db", "Banco de dados", null, null, null,
                    List.of("PostgreSQL, MySQL"), null))),
            new CvDocument.Section("experience", "Experiência profissional", false, List.of(
                new CvDocument.Entry("experience:0", "Engenheira", "Acme", null, "05/2021 – Atual",
                    List.of("Serviços em Java e Spring\nReduziu o tempo em 30%"), null),
                new CvDocument.Entry("experience:1", "Desenvolvedora", "Beta", null, "2019 – 2021",
                    List.of("APIs REST"), null))),
            new CvDocument.Section("degrees", "Formação acadêmica", false, List.of(
                new CvDocument.Entry("degrees:0", "Ciência da Computação", "USP", null, "2016 – 2020",
                    List.of(), null)))));
    AtsEvaluator evaluator = new AtsEvaluator(new ResumeParser());

    for (String name : CvTemplateCatalog.names()) {
      String template = new String(CvTemplateCatalog.class.getClassLoader()
          .getResourceAsStream("cv-templates/" + name + "-v1.typ").readAllBytes(), StandardCharsets.UTF_8);
      String source = TypstTemplateMaterializer.materialize(template, document);
      byte[] pdf = new TypstPdfRenderer("typst", new ObjectMapper()).render(source, document);

      AtsReport report = evaluator.evaluate(document, JobTerms.of(null, null), pdf);

      var parsing = report.checks().stream().filter(check -> check.id().equals("parsing")).findFirst().orElseThrow();
      assertEquals(25, parsing.points(), name + ": " + parsing.detail());
      var contact = report.checks().stream().filter(check -> check.id().equals("contact")).findFirst().orElseThrow();
      assertEquals(10, contact.points(), name + ": " + contact.detail());
    }
  }
}
