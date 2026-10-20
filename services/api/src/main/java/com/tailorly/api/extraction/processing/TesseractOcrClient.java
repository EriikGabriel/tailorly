package com.tailorly.api.extraction.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executa o binário local do Tesseract 5 em um processo separado para reconhecer o texto de
 * uma imagem renderizada a 300 DPI.
 *
 * <p>Cada chamada a {@link #recognize} cria um diretório temporário, escreve a imagem em PNG,
 * roda {@code tesseract} com OEM 1 (LSTM) e PSM 3 (segmentação automática de página) e lê o
 * resultado em {@code result.txt}. O processo tem timeout de 30 segundos
 * ({@code OCR_TIMEOUT}); se o executável não existir, falha com {@code OCR_UNAVAILABLE} em vez
 * de {@code OCR_FAILED}, para distinguir ambiente mal configurado de falha do próprio OCR. O
 * diretório e o processo (se ainda vivo) são sempre limpos no {@code finally}, mesmo quando a
 * thread é interrompida.</p>
 */
@Component
public class TesseractOcrClient implements OcrClient {
  private static final Logger log = LoggerFactory.getLogger(TesseractOcrClient.class);
  private final String executable;
  private final String languages;
  private final String revision;

  public TesseractOcrClient(
      @Value("${tailorly.extraction.tesseract-command:tesseract}") String executable,
      @Value("${tailorly.extraction.ocr-languages:por+eng}") String languages,
      @Value("${tailorly.extraction.ocr-revision:local-1}") String revision) {
    this.executable = executable;
    this.languages = languages;
    this.revision = revision;
  }

  /**
   * Identifica binário, modelos e parâmetros fixos de reconhecimento. Atualize
   * {@code TAILORLY_OCR_REVISION} ao trocar o Tesseract ou os arquivos {@code traineddata}
   * instalados, para invalidar execuções de extração já enfileiradas com a configuração antiga.
   */
  @Override
  public String fingerprint() {
    return "tesseract:" + revision + ":" + languages + ":oem1:psm3";
  }

  @Override
  public String recognize(BufferedImage image) {
    Path directory = null;
    Process process = null;
    try {
      directory = Files.createTempDirectory("tailorly-ocr-");
      Path input = directory.resolve("page.png");
      Path output = directory.resolve("result");
      ImageIO.write(image, "png", input.toFile());
      try {
        process = new ProcessBuilder(executable, input.toString(), output.toString(),
            "-l", languages, "--oem", "1", "--psm", "3")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      } catch (IOException ex) {
        throw new ExtractionFailure("OCR_UNAVAILABLE");
      }
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        throw new ExtractionFailure("OCR_TIMEOUT");
      }
      if (process.exitValue() != 0) {
        throw new ExtractionFailure("OCR_FAILED");
      }
      Path text = directory.resolve("result.txt");
      if (Files.size(text) > 1_000_000) {
        throw new ExtractionFailure("PAGE_TEXT_LIMIT_EXCEEDED");
      }
      return Files.readString(text, StandardCharsets.UTF_8);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ExtractionFailure("OCR_INTERRUPTED");
    } catch (IOException ex) {
      throw new ExtractionFailure("OCR_IO_FAILED");
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
        try {
          process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      if (directory != null) {
        for (String name : new String[]{"page.png", "result.txt", ""}) {
          try {
            Files.deleteIfExists(name.isEmpty() ? directory : directory.resolve(name));
          } catch (IOException ex) {
            log.warn("Falha ao limpar arquivo temporário de OCR");
          }
        }
      }
    }
  }
}
