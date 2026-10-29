package com.tailorly.api.cv.render;

import com.tailorly.api.cv.model.CvDocument;

/** Compila um template Typst com o conteúdo do currículo e devolve os bytes do PDF. */
public interface PdfRenderer {
  /** Identifica o compilador (por exemplo, {@code typst 0.13.1}) para registrar com cada geração. */
  String fingerprint();

  byte[] render(String templateSource, CvDocument document);
}
