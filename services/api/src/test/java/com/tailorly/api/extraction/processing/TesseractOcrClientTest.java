package com.tailorly.api.extraction.processing;

import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TesseractOcrClientTest {
  @Test
  @DisplayName("ausência do executável retorna erro de OCR sem marcar a thread interrompida")
  void missingExecutable() {
    var client = new TesseractOcrClient("/missing-tailorly-test/tesseract", "por+eng", "test");
    var image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    assertEquals("OCR_UNAVAILABLE", assertThrows(ExtractionFailure.class,
        () -> client.recognize(image)).getMessage());
    assertFalse(Thread.currentThread().isInterrupted());
  }
}
