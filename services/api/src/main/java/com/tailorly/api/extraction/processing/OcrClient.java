package com.tailorly.api.extraction.processing;

import java.awt.image.BufferedImage;

/**
 * Reconhecimento de texto em imagens renderizadas de páginas de PDF.
 *
 * <p>A única implementação atual é {@link TesseractOcrClient}, que delega a um executável
 * local. {@link #fingerprint()} identifica a configuração de OCR (binário/modelos/idiomas) e
 * compõe o fingerprint do pipeline em {@link PdfExtractor#versions()}; alterá-la invalida o
 * cache de execuções reaproveitadas.</p>
 */
public interface OcrClient {
  String recognize(BufferedImage image);

  String fingerprint();
}
