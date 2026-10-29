package com.tailorly.api.job.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.Builder;

/**
 * Detalhes de uma vaga lidos da página pública. Todo campo é opcional: só é preenchido quando a
 * página o declara de forma explícita; nada é inferido. As listas de seções ({@code requirements},
 * {@code niceToHave} etc.) são divisões textuais da descrição, ainda sem interpretação semântica.
 */
@Builder(toBuilder = true)
public record JobPostingResponse(
    String platform,
    String sourceUrl,
    String finalUrl,
    String canonicalUrl,
    Instant fetchedAt,
    String contentHash,
    String externalId,
    String title,
    String status,
    Company company,
    Location location,
    String workModel,
    String employmentType,
    String publishedAt,
    String expiresAt,
    Boolean expired,
    Salary salary,
    String descriptionText,
    List<String> responsibilities,
    List<String> requirements,
    List<String> niceToHave,
    List<String> benefits,
    List<String> skills,
    List<String> hiringProcess,
    List<String> warnings
) {
  public record Company(
      String name,
      String description,
      String website,
      String logoUrl,
      String careerPageUrl,
      Map<String, String> links
  ) {
    Company mergeWith(Company other) {
      if (other == null)
        return this;
      Map<String, String> mergedLinks = new LinkedHashMap<>();
      if (other.links != null)
        mergedLinks.putAll(other.links);
      if (links != null)
        mergedLinks.putAll(links);
      return new Company(
          first(name, other.name), first(description, other.description), first(website, other.website),
          first(logoUrl, other.logoUrl), first(careerPageUrl, other.careerPageUrl), mergedLinks);
    }
  }

  public record Location(String city, String state, String country, String raw) {
  }

  public record Salary(BigDecimal min, BigDecimal max, String currency, String period) {
  }

  /** Completa os campos vazios deste resultado com os de {@code fallback}; este tem prioridade. */
  public JobPostingResponse mergeWith(JobPostingResponse fallback) {
    if (fallback == null)
      return this;
    return new JobPostingResponse(
        first(platform, fallback.platform), first(sourceUrl, fallback.sourceUrl),
        first(finalUrl, fallback.finalUrl), first(canonicalUrl, fallback.canonicalUrl),
        first(fetchedAt, fallback.fetchedAt), first(contentHash, fallback.contentHash),
        first(externalId, fallback.externalId), first(title, fallback.title), first(status, fallback.status),
        company == null ? fallback.company : company.mergeWith(fallback.company),
        first(location, fallback.location), first(workModel, fallback.workModel),
        first(employmentType, fallback.employmentType), first(publishedAt, fallback.publishedAt),
        first(expiresAt, fallback.expiresAt), first(expired, fallback.expired), first(salary, fallback.salary),
        first(descriptionText, fallback.descriptionText),
        firstList(responsibilities, fallback.responsibilities), firstList(requirements, fallback.requirements),
        firstList(niceToHave, fallback.niceToHave), firstList(benefits, fallback.benefits),
        union(skills, fallback.skills), firstList(hiringProcess, fallback.hiringProcess),
        union(warnings, fallback.warnings));
  }

  private static <T> T first(T primary, T fallback) {
    return primary instanceof String text && text.isBlank() ? fallback : primary != null ? primary : fallback;
  }

  private static List<String> firstList(List<String> primary, List<String> fallback) {
    return primary != null && !primary.isEmpty() ? primary : fallback;
  }

  private static List<String> union(List<String> primary, List<String> fallback) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (primary != null)
      merged.addAll(primary);
    if (fallback != null)
      merged.addAll(fallback);
    return new ArrayList<>(merged);
  }
}
