package com.tailorly.api.cv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.pipeline.CvPipeline;
import com.tailorly.api.cv.template.CvTemplate;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.shared.util.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Impressão digital das entradas de uma geração (conteúdo, preferências, análise da vaga, versão
 * da esteira e template). Entradas idênticas reaproveitam a geração existente em vez de gastar cota.
 */
@Component
@RequiredArgsConstructor
class CvInputHasher {
  private static final String PIPELINE_INPUT_VERSION = CvPipeline.VERSION + "/" + "plan-1";

  private final ObjectMapper objectMapper;

  String hash(CvDocument document, CvPreferences preferences, CvTemplate template,
      JobPostingVersion jobVersion) {
    try {
      // A análise da vaga entra no hash porque ela orienta a IA e a avaliação de ATS.
      byte[] content = objectMapper.writeValueAsBytes(Arrays.asList(document, preferences,
          jobVersion.getAnalysis(), PIPELINE_INPUT_VERSION));
      return Sha256.hex(concat(content, template.getContentHash().getBytes(StandardCharsets.US_ASCII)));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not hash the CV content", exception);
    }
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] joined = new byte[first.length + second.length];
    System.arraycopy(first, 0, joined, 0, first.length);
    System.arraycopy(second, 0, joined, first.length, second.length);
    return joined;
  }
}
