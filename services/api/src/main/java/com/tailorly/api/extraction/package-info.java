/**
 * Pipeline de extração seletiva de texto/OCR de uma versão do Currículo Base.
 *
 * <p>{@link com.tailorly.api.extraction.ExtractionQueue} e
 * {@link com.tailorly.api.extraction.ExtractionWorker} orquestram o ciclo completo e por isso
 * ficam neste pacote, coordenando os dois agregados especializados:</p>
 * <ul>
 *   <li>{@link com.tailorly.api.extraction.run} — cada execução do pipeline para uma versão
 *       (fila, fingerprint, status e resultado).</li>
 *   <li>{@link com.tailorly.api.extraction.page} — o texto extraído de cada página de uma
 *       execução.</li>
 *   <li>{@link com.tailorly.api.extraction.processing} — a extração em si: PDFBox, OCR via
 *       Tesseract e a heurística que decide quando usar cada um.</li>
 * </ul>
 *
 * <p>Veja a seção "Extração seletiva de PDF" no {@code README.md} do serviço para o
 * comportamento completo (limites, timeouts, variáveis de ambiente do OCR e estados da fila).</p>
 */
package com.tailorly.api.extraction;
