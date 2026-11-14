package com.tailorly.api.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Execução de um CLI de IA local: diretório temporário isolado (sem CLAUDE.md/AGENTS.md do projeto),
 * variáveis de ambiente filtradas, prompt pelo stdin e timeout com {@code destroyForcibly}.
 */
final class LocalCliProcess {
  // Leituras bloqueantes de stdout/stderr ficam fora do pool comum do ForkJoin.
  private static final ExecutorService STREAM_READERS = Executors.newVirtualThreadPerTaskExecutor();

  record Result(int exitCode, String stdout, String stderr, long elapsedMillis) {
  }

  private LocalCliProcess() {
  }

  static Path createWorkDir(String prefix) throws IOException {
    return Files.createTempDirectory(prefix);
  }

  /** Lança {@link IllegalStateException} em timeout ou interrupção; {@code label} entra só na mensagem. */
  static Result run(String label, List<String> command, Path workDir, String stdin, Duration timeout,
      Predicate<String> dropEnvironmentVariable) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(command).directory(workDir.toFile());
    builder.environment().keySet().removeIf(dropEnvironmentVariable);

    long started = System.nanoTime();
    Process process = builder.start();
    CompletableFuture<String> stdout = readAsync(process.getInputStream());
    CompletableFuture<String> stderr = readAsync(process.getErrorStream());
    try (OutputStream input = process.getOutputStream()) {
      input.write((stdin == null ? "" : stdin).getBytes(StandardCharsets.UTF_8));
    } catch (IOException brokenPipe) {
      // O processo pode ter saído antes de ler tudo; o código de saída explica o motivo.
    }

    boolean finished;
    try {
      finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException interrupted) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the " + label, interrupted);
    }
    if (!finished) {
      process.destroyForcibly();
      throw new IllegalStateException(label + " timed out after " + timeout);
    }
    return new Result(process.exitValue(), stdout.join(), stderr.join(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
  }

  static void deleteQuietly(Path dir) {
    if (dir == null)
      return;
    try (var paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    } catch (IOException ignored) {
      // diretório temporário; o sistema limpa depois
    }
  }

  static String truncate(String value, int max) {
    if (value == null)
      return "";
    String trimmed = value.strip();
    return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "…";
  }

  private static CompletableFuture<String> readAsync(InputStream stream) {
    return CompletableFuture.supplyAsync(() -> {
      try (stream) {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException exception) {
        return "";
      }
    }, STREAM_READERS);
  }
}
