package com.tailorly.api.vault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copia os artefatos de uma geração (PDF e Markdown) para uma pasta local, por exemplo um vault
 * Obsidian como o Second Brain, quando {@code tailorly.vault.enabled=true}.
 *
 * <p>Só exporta gerações de contas — nunca de sessões anônimas, cujos dados são temporários e não
 * pertencem a um vault pessoal; o chamador decide isso antes de montar o {@link Request}. A cópia
 * Markdown exportada ganha um frontmatter YAML simples; o objeto canônico no Garage nunca é
 * alterado. {@link Request} carrega só dados simples (não a entidade JPA) porque a exportação roda
 * depois do commit da transação, quando qualquer referência preguiçosa já não pode ser carregada.
 * Qualquer falha de E/S é logada e absorvida: é um efeito colateral best-effort e nunca derruba a
 * resposta da geração.</p>
 */
public class VaultExportService {
  private static final Logger log = LoggerFactory.getLogger(VaultExportService.class);
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

  private final VaultExportProperties properties;

  public VaultExportService(VaultExportProperties properties) {
    this.properties = properties;
  }

  public record Request(
      UUID generationId,
      UUID ownerId,
      String jobTitle,
      String companyName,
      int atsScore,
      Instant generatedAt
  ) {
  }

  public void export(Request request, byte[] pdf, byte[] markdown) {
    if (properties.path() == null || properties.path().isBlank()) {
      log.warn("tailorly.vault.enabled is true but tailorly.vault.path is not set; skipping export");
      return;
    }
    try {
      Path targetDir = Path.of(properties.path(), properties.subfolder());
      Files.createDirectories(targetDir);

      String slug = slugOf(request);
      Files.write(targetDir.resolve(slug + ".pdf"), pdf);
      Files.writeString(targetDir.resolve(slug + ".md"),
          frontmatter(request) + new String(markdown, StandardCharsets.UTF_8), StandardCharsets.UTF_8);

      log.info("Exportando geração {} do owner {} para {}", request.generationId(), request.ownerId(), targetDir);
    } catch (IOException exception) {
      log.warn("Falha ao exportar geração {} para o vault: {}", request.generationId(), exception.getMessage());
    }
  }

  private String frontmatter(Request request) {
    return "---\n"
        + "title: " + yamlValue(request.jobTitle()) + "\n"
        + "company: " + yamlValue(request.companyName()) + "\n"
        + "generated_at: " + TIMESTAMP.format(request.generatedAt()) + "\n"
        + "ats_score: " + request.atsScore() + "\n"
        + "generation_id: " + request.generationId() + "\n"
        + "---\n\n";
  }

  private static String yamlValue(String value) {
    return value == null ? "\"\"" : "\"" + value.replace("\"", "'") + "\"";
  }

  private static String slugOf(Request request) {
    String base = ((request.companyName() != null ? request.companyName() + "-" : "")
        + (request.jobTitle() != null ? request.jobTitle() : "curriculo"))
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-|-$", "");
    String shortId = request.generationId().toString().substring(0, 8);
    return (base.isBlank() ? "curriculo" : base) + "-" + shortId;
  }
}
