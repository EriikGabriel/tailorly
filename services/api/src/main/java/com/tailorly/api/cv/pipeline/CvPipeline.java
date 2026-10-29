package com.tailorly.api.cv.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.cv.ats.AtsEvaluator;
import com.tailorly.api.cv.ats.AtsReport;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvPlan;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.model.JobTerms;
import com.tailorly.api.cv.render.PdfRenderer;
import com.tailorly.api.cv.template.TypstTemplateMaterializer;
import com.tailorly.api.job.dto.JobAnalysis;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Esteira de otimização para ATS. Monta o documento a partir dos fatos aprovados, compila o PDF,
 * mede a nota de ATS lendo o texto desse PDF e, enquanto a nota estiver abaixo da meta, pede à IA
 * (ou, sem IA, a regras simples) um novo plano de apresentação, recompila e mede de novo. Guarda a
 * melhor versão. Só a apresentação muda: o texto vem sempre dos fatos aprovados.
 */
@Component
@RequiredArgsConstructor
public class CvPipeline {
  public static final String VERSION = "cv-pipeline-1";
  private static final int MAX_REFINEMENTS = 2;
  private static final Logger log = LoggerFactory.getLogger(CvPipeline.class);

  private final PdfRenderer renderer;
  private final AtsEvaluator evaluator;
  private final CvPlanAdvisor advisor;

  /** Uma tentativa de layout: de onde veio o plano, a nota e o que mudou. */
  public record PipelineIteration(int number, String source, int score, String density, String rationale) {
  }

  /**
   * @param aiStatus {@code completed}, {@code unavailable} (provedor {@code rules}), {@code failed}
   *                 (a IA falhou e as regras assumiram) ou {@code skipped} (otimização desligada ou meta já atingida)
   */
  public record Outcome(CvDocument document, byte[] pdf, AtsReport report, List<PipelineIteration> iterations,
      String aiStatus) {
  }

  public Outcome run(UUID ownerId, CvDocument base, String templateSource, JobTerms terms, JobAnalysis analysis,
      String jobTitle, CvPreferences prefs) {
    CvPlan plan = CvPlan.neutral();
    String editableTemplate = TypstTemplateMaterializer.materialize(templateSource, base);
    Candidate best = attempt(base, plan, editableTemplate, terms);
    List<PipelineIteration> iterations = new ArrayList<>();
    iterations.add(new PipelineIteration(0, "base", best.report().score(), best.document().layout().density(), null));
    String aiStatus = "skipped";

    boolean aiAvailable = prefs.optimizeForAts() && advisor.isAvailable(ownerId);
    if (prefs.optimizeForAts() && !aiAvailable) {
      aiStatus = "unavailable";
    }
    Candidate current = best;
    for (int round = 1; prefs.optimizeForAts() && round <= MAX_REFINEMENTS
        && best.report().score() < AtsEvaluator.TARGET_SCORE; round++) {
      CvPlan next = null;
      String source = "rules";
      String rationale = null;
      if (aiAvailable && !"failed".equals(aiStatus)) {
        try {
          CvPlanAdvisor.Suggestion suggestion = advisor.suggest(ownerId, base, analysis, jobTitle, prefs,
              current.report());
          next = suggestion.plan();
          source = "ai";
          rationale = suggestion.rationale();
          aiStatus = "completed";
        } catch (RuntimeException failure) {
          // A falha da IA não derruba a geração: as regras assumem e o status fica registrado.
          log.warn("Resume layout planning by AI failed; falling back to rules", failure);
          aiStatus = "failed";
        }
      }
      if (next == null) {
        next = rulesPlan(current);
      }
      if (next == null) {
        break;
      }
      current = attempt(base, next, editableTemplate, terms);
      iterations.add(new PipelineIteration(round, source, current.report().score(), current.document().layout().density(),
          rationale));
      if (current.report().score() > best.report().score()) {
        best = current;
      }
    }
    return new Outcome(best.document(), best.pdf(), best.report(), List.copyOf(iterations), aiStatus);
  }

  /** Sem IA: o único ajuste seguro é compactar quando o currículo passou de duas páginas ou de mil palavras. */
  private static CvPlan rulesPlan(Candidate current) {
    boolean tooLong = (current.report().pages() != null && current.report().pages() > 2)
        || current.report().words() > 1000;
    if (!tooLong || "compact".equals(current.document().layout().density())) {
      return null;
    }
    return current.plan().withDensity("compact");
  }

  private Candidate attempt(CvDocument base, CvPlan plan, String templateSource, JobTerms terms) {
    CvDocument document = plan.apply(base);
    byte[] pdf = renderer.render(templateSource, document);
    return new Candidate(plan, document, pdf, evaluator.evaluate(document, terms, pdf));
  }

  private record Candidate(CvPlan plan, CvDocument document, byte[] pdf, AtsReport report) {
  }
}
