package com.tailorly.api.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultExportServiceTest {

  @Test
  void writesPdfAndFrontmatterMarkdownUnderSubfolder(@TempDir Path vaultRoot) throws IOException {
    VaultExportService service = new VaultExportService();
    VaultExportService.Request request = new VaultExportService.Request(
        UUID.randomUUID(), UUID.randomUUID(), "Engenheiro de Software", "Acme",
        87, Instant.parse("2026-01-01T00:00:00Z"), vaultRoot.resolve("B03 Resources/CVs"));

    service.export(request, "%PDF-fake".getBytes(StandardCharsets.UTF_8), "# Currículo".getBytes(StandardCharsets.UTF_8));

    Path subfolder = vaultRoot.resolve("B03 Resources/CVs");
    var names = listFileNames(subfolder);
    assertEquals(2, names.size());
    assertTrue(names.get(0).startsWith("acme-engenheiro-de-software-"));

    Path markdown = subfolder.resolve(names.stream().filter(name -> name.endsWith(".md")).findFirst().orElseThrow());
    String content = Files.readString(markdown, StandardCharsets.UTF_8);
    assertTrue(content.startsWith("---\n"));
    assertTrue(content.contains("ats_score: 87"));
    assertTrue(content.contains("# Currículo"));
  }

  @Test
  void writesNothingWhenTheOwnerHasNoExportTarget(@TempDir Path vaultRoot) throws IOException {
    VaultExportService service = new VaultExportService();
    VaultExportService.Request request = new VaultExportService.Request(
        UUID.randomUUID(), UUID.randomUUID(), "Title", "Company", 50, Instant.now(), null);

    service.export(request, "pdf".getBytes(StandardCharsets.UTF_8), "md".getBytes(StandardCharsets.UTF_8));

    assertTrue(listFileNames(vaultRoot).isEmpty());
  }

  private static java.util.List<String> listFileNames(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return java.util.List.of();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    }
  }
}
