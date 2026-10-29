package com.tailorly.api.cv.pipeline;

import java.util.List;

import com.tailorly.api.cv.ats.AtsReport;

/** O que a esteira registra por geração: a nota final, as tentativas de layout e o estado da etapa de IA. */
public record CvAtsResult(AtsReport report, List<CvPipeline.PipelineIteration> iterations, String aiStatus) {
}
