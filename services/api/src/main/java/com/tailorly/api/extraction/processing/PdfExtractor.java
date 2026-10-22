package com.tailorly.api.extraction.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extração seletiva de texto de PDF: usa o texto nativo quando ele é bom o suficiente
 * ({@link TextQualityPolicy#accepts}) e recorre a OCR página a página quando não é.
 *
 * <p>{@link #versions()} e {@link #fingerprint()} identificam de forma determinística a
 * configuração inteira do pipeline (implementação, PDFBox, política de qualidade,
 * normalizador, OCR, DPI e limites). O fingerprint é usado por
 * {@link com.tailorly.api.extraction.ExtractionQueue} para decidir se uma execução enfileirada
 * pode ser reaproveitada ou se mudou o suficiente para precisar de uma nova.</p>
 */
@Component
public class PdfExtractor {
  public static final String IMPLEMENTATION = "selective-pdf-v2";
  private final OcrClient ocr;
  private final TextQualityPolicy quality;

  public PdfExtractor(OcrClient ocr, TextQualityPolicy quality) {
    this.ocr = ocr;
    this.quality = quality;
  }

  /** Versões/parâmetros de cada etapa do pipeline; a chave "ocr" delega ao {@link OcrClient} atual. */
  public Map<String, Object> versions() {
    return Map.of("implementation", IMPLEMENTATION, "pdfbox", "3.0.8",
        "qualityPolicy", "2", "normalizer", "1", "ocr", ocr.fingerprint(),
        "dpi", 300, "maxPages", 20, "maxPixels", 20_000_000);
  }

  /** Hash determinístico de {@link #versions()}, usado para deduplicar execuções na fila. */
  public String fingerprint() {
    String canonical = versions().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(java.util.stream.Collectors.joining("\n"));
    return hash(canonical.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Extrai o texto de cada página do PDF.
   *
   * <p>Para cada página: extrai texto nativo ordenado por posição
   * ({@link org.apache.pdfbox.text.PDFTextStripper#setSortByPosition}); se
   * {@link TextQualityPolicy#accepts} rejeitar esse texto, renderiza a página a 300 DPI
   * (respeitando o limite de {@code 20_000_000} pixels) e roda OCR. Um OCR vazio não substitui
   * um texto nativo curto porém presente — preferimos manter algum texto a descartá-lo. Falha
   * cedo com códigos específicos ({@code PDF_SIZE_LIMIT_EXCEEDED}, {@code PDF_ENCRYPTED},
   * {@code PDF_PAGE_LIMIT_EXCEEDED}, {@code EXTRACTION_TIMEOUT} após 3 minutos,
   * {@code PAGE_TEXT_LIMIT_EXCEEDED}) e falha no final com {@code PDF_NO_TEXT} se nenhuma
   * página produziu texto útil.</p>
   */
  public List<PageText> extract(byte[] bytes) {
    if (bytes.length > 7 * 1024 * 1024) throw new ExtractionFailure("PDF_SIZE_LIMIT_EXCEEDED");
    long deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos();

    try (PDDocument document = Loader.loadPDF(bytes)) {
      if (document.isEncrypted()) throw new ExtractionFailure("PDF_ENCRYPTED");

      int count = document.getNumberOfPages();
      if (count == 0 || count > 20) throw new ExtractionFailure("PDF_PAGE_LIMIT_EXCEEDED");

      PDFRenderer renderer = new PDFRenderer(document);
      List<PageText> pages = new ArrayList<>();
      for (int index = 0; index < count; index++) {
        if (System.nanoTime() > deadline) throw new ExtractionFailure("EXTRACTION_TIMEOUT");

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(index + 1);
        stripper.setEndPage(index + 1);
        stripper.setSortByPosition(true);
        stripper.setShouldSeparateByBeads(true);
        stripper.setSuppressDuplicateOverlappingText(true);
        String text = quality.normalize(stripper.getText(document));
        String method = "DIRECT";

        // Texto nativo insuficiente: renderiza a página e tenta OCR como alternativa.
        if (!quality.accepts(text)) {
          var box = document.getPage(index).getCropBox();
          double width = Math.ceil(box.getWidth() * 300.0 / 72);
          double height = Math.ceil(box.getHeight() * 300.0 / 72);

          if (!Double.isFinite(width * height) || width <= 0 || height <= 0
              || width * height > 20_000_000) {
            throw new ExtractionFailure("PDF_RENDER_LIMIT_EXCEEDED");
          }

          var image = renderer.renderImageWithDPI(index, 300, ImageType.RGB);

          try {
            String recognized = quality.normalize(ocr.recognize(image));
            // Um OCR vazio não deve apagar texto nativo de uma página curta.
            if (!recognized.isBlank() || text.isBlank()) {
              text = recognized;
              method = "OCR";
            }
          } finally {
            image.flush();
          }
        }

        if (text.length() > 250_000) throw new ExtractionFailure("PAGE_TEXT_LIMIT_EXCEEDED");
        pages.add(new PageText(index + 1, method, text));
      }

      if (pages.stream().allMatch(page -> page.text().isBlank())) {
        throw new ExtractionFailure("PDF_NO_TEXT");
      }

      return List.copyOf(pages);
    } catch (InvalidPasswordException ex) {
      throw new ExtractionFailure("PDF_ENCRYPTED");
    } catch (IOException ex) {
      throw new ExtractionFailure("PDF_READ_FAILED");
    }
  }

  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public record PageText(int number, String method, String text) {}
}
