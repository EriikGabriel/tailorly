package com.tailorly.api.cv.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ServiceUnavailableException;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executa o CLI do Typst em um processo separado, como o OCR faz com o Tesseract.
 *
 * <p>O conteúdo do currículo nunca é interpolado no código Typst: ele vai em {@code data.json},
 * lido pelo template com {@code json()}, então um valor com sintaxe Typst é apenas texto. A
 * compilação roda em um diretório temporário usado como {@code --root} (o template não lê nada
 * fora dele), tem timeout e o diretório é sempre removido.</p>
 */
@Component
public class TypstPdfRenderer implements PdfRenderer {
  private static final Logger log = LoggerFactory.getLogger(TypstPdfRenderer.class);
  private static final int TIMEOUT_SECONDS = 30;

  private final String executable;
  private final ObjectMapper objectMapper;
  private volatile String fingerprint;

  public TypstPdfRenderer(@Value("${tailorly.generation.typst-command:typst}") String executable,
      ObjectMapper objectMapper) {
    this.executable = resolveExecutable(executable);
    this.objectMapper = objectMapper;
  }

  private static String resolveExecutable(String executable) {
    Path path = Path.of(executable);
    return path.getNameCount() > 1 && !path.isAbsolute()
        ? path.toAbsolutePath().normalize().toString()
        : executable;
  }

  @Override
  public String fingerprint() {
    String cached = fingerprint;
    if (cached == null) {
      cached = executable + ":" + version();
      fingerprint = cached;
    }
    return cached;
  }

  @Override
  public byte[] render(String templateSource, CvDocument document) {
    Path directory = null;
    try {
      directory = Files.createTempDirectory("tailorly-cv-");
      Files.writeString(directory.resolve("main.typ"), templateSource, StandardCharsets.UTF_8);
      Files.write(directory.resolve("data.json"), objectMapper.writeValueAsBytes(document));
      run(directory, "compile", "--root", directory.toString(), "main.typ", "out.pdf");
      return Files.readAllBytes(directory.resolve("out.pdf"));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize the CV content", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not run the PDF compiler", exception);
    } finally {
      delete(directory);
    }
  }

  private String version() {
    Path directory = null;
    try {
      directory = Files.createTempDirectory("tailorly-cv-");
      run(directory, "--version");
      return Files.readString(directory.resolve("stdout.txt"), StandardCharsets.UTF_8).strip();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read the PDF compiler version", exception);
    } finally {
      delete(directory);
    }
  }

  /** Roda o Typst em {@code directory}; a saída padrão vai para {@code stdout.txt} e o erro para {@code stderr.txt}. */
  private void run(Path directory, String... arguments) throws IOException {
    String[] command = new String[arguments.length + 1];
    command[0] = executable;
    System.arraycopy(arguments, 0, command, 1, arguments.length);
    Process process;
    try {
      process = new ProcessBuilder(command)
          .directory(directory.toFile())
          .redirectOutput(directory.resolve("stdout.txt").toFile())
          .redirectError(directory.resolve("stderr.txt").toFile())
          .start();
    } catch (IOException exception) {
      throw new ServiceUnavailableException(ErrorCodes.CvGeneration.RENDERER_UNAVAILABLE);
    }
    try {
      if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new UnprocessableRequestException(ErrorCodes.CvGeneration.TEMPLATE_INVALID,
            List.of("A compilação excedeu " + TIMEOUT_SECONDS + " segundos"));
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("PDF compilation was interrupted", exception);
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
    if (process.exitValue() != 0) {
      log.warn("PDF compilation failed with exit code {}", process.exitValue());
      // Os diagnósticos vão a quem escreveu o template (a prévia os mostra no editor), sem o
      // diretório temporário e limitados em tamanho.
      String stderr = Files.readString(directory.resolve("stderr.txt"), StandardCharsets.UTF_8)
          .replace(directory.toString() + "/", "");
      List<String> diagnostics = stderr.lines().filter(line -> !line.isBlank()).limit(40)
          .map(line -> line.length() > 300 ? line.substring(0, 300) : line).toList();
      throw new UnprocessableRequestException(ErrorCodes.CvGeneration.TEMPLATE_INVALID, diagnostics);
    }
  }

  private static void delete(Path directory) {
    if (directory == null) {
      return;
    }
    try (var files = Files.walk(directory)) {
      files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException exception) {
          log.warn("Could not remove temporary PDF compilation file");
        }
      });
    } catch (IOException exception) {
      log.warn("Could not clean up temporary PDF compilation directory");
    }
  }
}
