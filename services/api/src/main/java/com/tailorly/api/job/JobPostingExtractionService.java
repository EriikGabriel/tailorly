package com.tailorly.api.job;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Obtém a página da vaga e combina os extratores por ordem de confiabilidade: Gupy (dados
 * estruturados da própria plataforma), JSON-LD {@code JobPosting} e, por último, o HTML genérico.
 * Cada extrator só completa o que o anterior não encontrou. O resultado não é persistido e não passa
 * por IA; ele descreve apenas o que a página declara.
 */
@Service
@RequiredArgsConstructor
public class JobPostingExtractionService {
  private final JobPageFetcher fetcher;
  private final GupyJobExtractor gupyExtractor;
  private final JsonLdJobExtractor jsonLdExtractor;
  private final GenericHtmlJobExtractor genericExtractor;
  private final PastedTextJobExtractor textExtractor;

  public JobPostingResponse extract(String url) {
    FetchedPage page = fetcher.fetch(url);

    JobPostingResponse merged = null;
    for (JobPageExtractor extractor : List.of(gupyExtractor, jsonLdExtractor, genericExtractor)) {
      Optional<JobPostingResponse> extracted = extractor.extract(page.document(), page.finalUrl());
      if (extracted.isPresent())
        merged = merged == null ? extracted.get() : merged.mergeWith(extracted.get());
    }

    if (merged == null || isUnusable(merged))
      throw new UnprocessableRequestException(ErrorCodes.Job.NO_CONTENT);
    return finalizeResponse(merged, url.trim(), page.finalUrl());
  }

  /** Texto colado: nada é baixado; as seções vêm dos títulos presentes no próprio texto. */
  public JobPostingResponse extractFromText(String text, String title, String company) {
    JobPostingResponse extracted = textExtractor.extract(text, title, company);
    if (isUnusable(extracted))
      throw new UnprocessableRequestException(ErrorCodes.Job.NO_CONTENT);
    return finalizeResponse(extracted, null, null);
  }

  // Páginas renderizadas por JavaScript ou atrás de login entregam só o casco; um título solto
  // ("Entrar | Site") seria apresentado como se fosse uma vaga.
  private static boolean isUnusable(JobPostingResponse response) {
    boolean noText = response.descriptionText() == null || response.descriptionText().isBlank();
    boolean genericOnly = "generic".equals(response.platform());
    boolean limited = response.warnings() != null && response.warnings().contains("limited_content");
    return (noText && (response.title() == null || response.title().isBlank())) || (genericOnly && limited);
  }

  private static JobPostingResponse finalizeResponse(JobPostingResponse merged, String sourceUrl, String finalUrl) {
    Instant now = Instant.now();
    Instant expiresAt = parseInstant(merged.expiresAt());
    boolean expired = expiresAt != null && expiresAt.isBefore(now);

    List<String> warnings = new ArrayList<>(orEmpty(merged.warnings()));
    // O aviso do extrator genérico só vale quando nenhum extrator melhor reconheceu a página.
    if (!"generic".equals(merged.platform()))
      warnings.remove("limited_content");
    if (expired && !warnings.contains("expired"))
      warnings.add("expired");
    if (orEmpty(merged.requirements()).isEmpty() && orEmpty(merged.responsibilities()).isEmpty()
        && orEmpty(merged.niceToHave()).isEmpty())
      warnings.add("sections_not_detected");

    return merged.toBuilder()
        .sourceUrl(sourceUrl)
        .finalUrl(finalUrl)
        .fetchedAt(now)
        .contentHash(versionHash(merged))
        .expired(expiresAt == null ? null : expired)
        .descriptionText(merged.descriptionText() == null ? "" : merged.descriptionText())
        .responsibilities(orEmpty(merged.responsibilities()))
        .requirements(orEmpty(merged.requirements()))
        .niceToHave(orEmpty(merged.niceToHave()))
        .benefits(orEmpty(merged.benefits()))
        .skills(orEmpty(merged.skills()))
        .hiringProcess(orEmpty(merged.hiringProcess()))
        .warnings(warnings)
        .build();
  }

  /** Recalcula {@code expired} e o aviso correspondente; o prazo muda de significado com o tempo. */
  public static JobPostingResponse refreshExpiry(JobPostingResponse job) {
    Instant expiresAt = parseInstant(job.expiresAt());
    Boolean expired = expiresAt == null ? null : expiresAt.isBefore(Instant.now());
    List<String> warnings = new ArrayList<>(orEmpty(job.warnings()));
    warnings.remove("expired");
    if (Boolean.TRUE.equals(expired))
      warnings.add("expired");
    return job.toBuilder().expired(expired).warnings(warnings).build();
  }

  private static <T> List<T> orEmpty(List<T> list) {
    return list == null ? List.of() : list;
  }

  /** Aceita data-hora ISO-8601 ou só a data; uma data vale até o fim desse dia (UTC). */
  static Instant parseInstant(String value) {
    if (value == null || value.isBlank())
      return null;
    String trimmed = value.trim();
    try {
      return OffsetDateTime.parse(trimmed).toInstant();
    } catch (DateTimeParseException ignored) {
      // tenta os formatos mais simples abaixo
    }
    try {
      return LocalDate.parse(trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed)
          .plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  /**
   * Hash do conteúdo que define uma versão da vaga: texto e campos estruturados que mudam o
   * significado (prazo, salário, local etc.), mas não metadados da leitura como data ou avisos.
   */
  static String versionHash(JobPostingResponse job) {
    if (job.descriptionText() == null || job.descriptionText().isBlank())
      return null;
    String canonical = String.join("\u001f",
        nullToEmpty(job.title()), nullToEmpty(job.workModel()), nullToEmpty(job.employmentType()),
        nullToEmpty(job.expiresAt()), String.valueOf(job.location()), String.valueOf(job.salary()),
        job.company() == null ? "" : nullToEmpty(job.company().name()),
        String.join("\u001e", orEmpty(job.hiringProcess())), job.descriptionText().strip());
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
