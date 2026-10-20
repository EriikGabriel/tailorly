package com.tailorly.api.extraction.processing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfExtractorTest {
  private OcrClient ocr;
  private PdfExtractor extractor;
  private static final String TEXT = "Pessoa fictícia com experiência em desenvolvimento Java e testes de software.";

  @BeforeEach
  void setup() {
    ocr = mock(OcrClient.class);
    extractor = new PdfExtractor(ocr, new TextQualityPolicy());
  }

  @Test
  @DisplayName("PDF digital preserva acentos e dispensa OCR")
  void direct() throws Exception {
    var pages = extractor.extract(pdf(false, false));
    assertEquals(1, pages.size());
    assertEquals(TEXT, pages.getFirst().text());
    assertEquals("DIRECT", pages.getFirst().method());
    verifyNoInteractions(ocr);
  }

  @Test
  @DisplayName("PDF misto usa OCR somente na página escaneada")
  void mixed() throws Exception {
    when(ocr.recognize(any())).thenReturn("Texto fictício reconhecido na página escaneada.");
    var pages = extractor.extract(pdf(true, true));
    assertEquals(2, pages.size());
    assertEquals("DIRECT", pages.get(0).method());
    assertEquals("OCR", pages.get(1).method());
    assertEquals(2, pages.get(1).number());
    verify(ocr, times(1)).recognize(any());
  }

  @Test
  @DisplayName("falha do OCR impede conclusão parcial do documento")
  void failedOcr() throws Exception {
    when(ocr.recognize(any())).thenThrow(new ExtractionFailure("OCR_TIMEOUT"));
    byte[] bytes = pdf(true, true);
    assertEquals("OCR_TIMEOUT", assertThrows(ExtractionFailure.class, () -> extractor.extract(bytes)).getMessage());
  }

  @Test
  @DisplayName("PDF corrompido falha sem chamar OCR")
  void corrupt() {
    assertThrows(ExtractionFailure.class, () -> extractor.extract("%PDF-inválido".getBytes()));
    verifyNoInteractions(ocr);
  }

  @Test
  @DisplayName("página com dimensões excessivas não é renderizada")
  void oversizedPage() throws Exception {
    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage(new PDRectangle(10000, 10000)));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      assertEquals("PDF_RENDER_LIMIT_EXCEEDED", assertThrows(ExtractionFailure.class,
          () -> extractor.extract(output.toByteArray())).getMessage());
      verifyNoInteractions(ocr);
    }
  }

  @Test
  @DisplayName("resultado sem texto não é considerado extração útil")
  void emptyOcr() throws Exception {
    when(ocr.recognize(any())).thenReturn("");
    byte[] bytes = pdf(true, false);
    assertEquals("PDF_NO_TEXT", assertThrows(ExtractionFailure.class, () -> extractor.extract(bytes)).getMessage());
  }

  @Test
  @DisplayName("alterar a revisão do OCR altera a chave de reaproveitamento")
  void fingerprint() {
    when(ocr.fingerprint()).thenReturn("ocr-v1");
    String first = extractor.fingerprint();
    assertEquals(first, extractor.fingerprint());
    when(ocr.fingerprint()).thenReturn("ocr-v2");
    assertNotEquals(first, extractor.fingerprint());
  }

  private byte[] pdf(boolean scan, boolean mixed) throws Exception {
    try (PDDocument document = new PDDocument()) {
      if (!scan || mixed) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
          stream.beginText();
          stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
          stream.newLineAtOffset(30, 700);
          stream.showText(TEXT);
          stream.endText();
        }
      }
      if (scan) {
        PDPage page = new PDPage();
        document.addPage(page);
        BufferedImage image = new BufferedImage(600, 100, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 600, 100);
        graphics.setColor(Color.BLACK);
        graphics.drawString("Currículo sintético para teste de OCR", 10, 50);
        graphics.dispose();
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
          stream.drawImage(LosslessFactory.createFromImage(document, image), 30, 600);
        }
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }
}
