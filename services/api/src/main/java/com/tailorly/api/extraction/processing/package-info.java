/**
 * Implementação da extração seletiva de PDF: {@link com.tailorly.api.extraction.processing.PdfExtractor}
 * decide, página a página, entre texto nativo (PDFBox) e OCR
 * ({@link com.tailorly.api.extraction.processing.TesseractOcrClient}), usando a heurística de
 * {@link com.tailorly.api.extraction.processing.TextQualityPolicy}.
 * {@link com.tailorly.api.extraction.processing.ExtractionFailure} é a exceção com código de
 * erro nomeado usada em todo o pipeline.
 */
package com.tailorly.api.extraction.processing;
