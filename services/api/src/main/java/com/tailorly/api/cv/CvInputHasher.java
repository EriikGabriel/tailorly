package com.tailorly.api.cv;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
      JobPostingVersion jobVersion, List<UUID> confirmedEvidenceIds) {
    try {
      // A análise da vaga entra no hash porque ela orienta a IA e a avaliação de ATS. As evidências
      // confirmadas entram só quando existem, para que a ausência delas não mude o hash das gerações antigas.
      List<Object> inputs = new ArrayList<>(Arrays.asList(document, preferences,
          jobVersion.getAnalysis(), PIPELINE_INPUT_VERSION));
      if (!confirmedEvidenceIds.isEmpty()) {
        inputs.add(confirmedEvidenceIds.stream().map(UUID::toString).sorted().toList());
      }
      byte[] content = objectMapper.writeValueAsBytes(inputs);
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
