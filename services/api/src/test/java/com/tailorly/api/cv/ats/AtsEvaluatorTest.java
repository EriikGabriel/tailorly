package com.tailorly.api.cv.ats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tailorly.api.cv.TestPdfs;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AtsEvaluatorTest {
  private final AtsEvaluator evaluator = new AtsEvaluator();

  private static CvDocument document() {
    return new CvDocument("Ana Souza", "Engenheira", null,
        List.of(new CvDocument.Contact("ana@example.com", "mailto:ana@example.com"),
            new CvDocument.Contact("(11) 98765-4321", null)),
        List.of(
            new CvDocument.Section("technologies", "Tecnologias", true, List.of(
                new CvDocument.Entry("technologies:0", "Java", null, null, null, List.of(), null))),
            new CvDocument.Section("experience", "Experiência profissional", false, List.of(
                new CvDocument.Entry("experience:0", "Engenheira", "Acme", null, "05/2021 – Atual",
                    List.of("Serviços em Java e Spring"), null))),
            new CvDocument.Section("degrees", "Formação acadêmica", false, List.of(
                new CvDocument.Entry("degrees:0", "Ciência da Computação", "USP", null, "2016 – 2020", List.of(), null)))));
  }

  private static JobTerms terms() {
    JobAnalysis analysis = JobAnalysis.builder().technologies(List.of("Java", "Kubernetes"))
        .requirements(List.of(new JobAnalysis.Requirement("Java", "required", "technology", null,
            List.of("Java"), "Java"))).build();
    return JobTerms.of(JobPostingResponse.builder().skills(List.of("Spring")).build(), analysis);
  }

  @Test
  @DisplayName("lê o texto do PDF e pontua cobertura, estrutura, leitura e datas")
  void scoresFromExtractedPdfText() {
    CvDocument document = document();

    AtsReport report = evaluator.evaluate(document, terms(), TestPdfs.of(document));

    assertEquals(List.of("Java", "Spring"), report.matchedTerms());
    assertEquals(List.of("Kubernetes"), report.missingTerms());
    assertEquals(1, report.pages());
    assertEquals(report.score(), report.checks().stream().mapToInt(AtsReport.AtsCheck::points).sum());
    // Java pesa 2 (obrigatório), Spring 1, Kubernetes 1: 3 de 4 de 40 pontos.
    assertEquals(30, check(report, "keywords").points());
    assertEquals(10, check(report, "contact").points());
    assertEquals(15, check(report, "sections").points());
    assertEquals(20, check(report, "parsing").points());
    assertEquals(5, check(report, "dates").points());
    // O texto é curto demais para a faixa de palavras esperada: só as páginas contam.
    assertEquals(5, check(report, "length").points());
  }

  @Test
  @DisplayName("PDF ilegível zera a leitura e usa o texto do documento como alternativa")
  void unreadablePdfScoresZeroOnParsing() {
    AtsReport report = evaluator.evaluate(document(), terms(), "não é um pdf".getBytes());

    assertEquals(0, check(report, "parsing").points());
    assertTrue(check(report, "parsing").detail().contains("Não foi possível"));
    assertEquals(List.of("Java", "Spring"), report.matchedTerms());
  }

  @Test
  @DisplayName("seções fora de ordem no texto, sem contato ou muito longo perdem pontos")
  void penalizesStructuralProblems() {
    CvDocument bare = new CvDocument(null, null, null, List.of(), List.of());

    AtsReport report = evaluator.evaluate(bare, JobTerms.of(null, null), TestPdfs.of("curto", 70));

    assertEquals(0, check(report, "contact").points());
    assertEquals(0, check(report, "sections").points());
    assertEquals(40, check(report, "keywords").points(), "sem termos na vaga não há o que cobrar");

    String longText = ("palavra ".repeat(20) + "\n").repeat(70);
    AtsReport tooLong = evaluator.evaluate(document(), terms(), TestPdfs.of(longText, 20));
    assertEquals(0, check(tooLong, "length").points());
  }

  private static AtsReport.AtsCheck check(AtsReport report, String id) {
    return report.checks().stream().filter(check -> check.id().equals(id)).findFirst().orElseThrow();
  }
}
