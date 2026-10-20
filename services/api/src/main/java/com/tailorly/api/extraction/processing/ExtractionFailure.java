package com.tailorly.api.extraction.processing;

/**
 * Falha esperada e nomeada do pipeline de extração (por exemplo {@code PDF_ENCRYPTED},
 * {@code PDF_NO_TEXT}, {@code OCR_UNAVAILABLE}, {@code PIPELINE_CHANGED}).
 *
 * <p>A mensagem é o próprio código de erro: {@link com.tailorly.api.extraction.ExtractionWorker}
 * usa {@code getMessage()} diretamente como {@code errorCode} da {@code ExtractionRun}, sem
 * tradução adicional. Para falhas não previstas, o worker usa o estágio em que ocorreram no
 * lugar de um código específico.</p>
 */
public class ExtractionFailure extends RuntimeException {
  public ExtractionFailure(String code) {
    super(code);
  }
}
