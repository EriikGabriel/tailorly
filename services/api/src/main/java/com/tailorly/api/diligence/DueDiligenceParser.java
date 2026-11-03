package com.tailorly.api.diligence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.diligence.dto.DueDiligenceReport.FactorResult;
import com.tailorly.api.diligence.dto.DueDiligenceReport.Finding;
import com.tailorly.api.diligence.dto.DueDiligenceReport.Uncertainty;

/**
 * Valida a resposta do modelo. Uma conclusão só entra no relatório se citar uma fonte candidata do próprio
 * fator e um trecho que realmente apareça no título ou resumo dessa fonte; o resto é descartado. Relatos
 * de pessoas (avaliações, fóruns) e fontes que podem tratar de outra empresa nunca têm incerteza baixa.
 * A ausência de conclusões válidas vira {@code no_data}, jamais um problema.
 */
final class DueDiligenceParser {
  static final String EVIDENCE_FOUND = "evidence_found";
  static final String NO_DATA = "no_data";
  static final String SOURCE_UNAVAILABLE = "source_unavailable";

  private static final Set<String> SOURCE_TYPES = Set.of("official", "company", "news", "review", "other");
  private static final Set<String> POLARITIES = Set.of("positive", "negative", "neutral");
  private static final Set<String> LEVELS = Set.of("low", "medium", "high");
  private static final int MAX_FINDINGS = 4;
  private static final int MAX_TEXT = 400;

  private final ObjectMapper mapper;

  DueDiligenceParser(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * @param candidates fontes candidatas por fator; fatores cuja busca falhou ficam em {@code unavailable}
   * @return sempre os onze fatores, na ordem do enum
   */
  List<FactorResult> parse(String response, Map<DiligenceFactor, List<DiligenceSource>> candidates,
      Set<DiligenceFactor> unavailable, Instant retrievedAt) {
    JsonNode root;
    try {
      root = mapper.readTree(AiChatClient.stripCodeFence(response));
    } catch (Exception exception) {
      throw new IllegalStateException("Due diligence response is not valid JSON", exception);
    }
    if (root == null || !root.path("factors").isArray())
      throw new IllegalStateException("Due diligence response has no factors array");

    Map<DiligenceFactor, JsonNode> byFactor = new LinkedHashMap<>();
    for (JsonNode node : root.path("factors")) {
      DiligenceFactor factor = DiligenceFactor.fromId(node.path("factor").asText(""));
      if (factor != null)
        byFactor.putIfAbsent(factor, node);
    }

    List<FactorResult> results = new ArrayList<>();
    for (DiligenceFactor factor : DiligenceFactor.all()) {
      if (unavailable.contains(factor)) {
        results.add(new FactorResult(factor.id(), SOURCE_UNAVAILABLE, "",
            new Uncertainty("high", ""), List.of()));
        continue;
      }
      results.add(parseFactor(factor, byFactor.get(factor), candidates.getOrDefault(factor, List.of()), retrievedAt));
    }
    return results;
  }

  /** Fator sem nenhuma fonte candidata: não há o que perguntar ao modelo. */
  static FactorResult noData(DiligenceFactor factor) {
    return new FactorResult(factor.id(), NO_DATA, "", new Uncertainty("high", ""), List.of());
  }

  private FactorResult parseFactor(DiligenceFactor factor, JsonNode node, List<DiligenceSource> sources,
      Instant retrievedAt) {
    if (node == null || sources.isEmpty())
      return noData(factor);

    Map<Integer, DiligenceSource> byId = new LinkedHashMap<>();
    sources.forEach(source -> byId.put(source.id(), source));

    List<Finding> findings = new ArrayList<>();
    for (JsonNode item : node.path("findings")) {
      if (findings.size() >= MAX_FINDINGS)
        break;
      DiligenceSource source = byId.get(item.path("sourceId").asInt(-1));
      String claim = text(item, "claim");
      String excerpt = text(item, "excerpt");
      if (source == null || claim.isEmpty() || excerpt.length() < 8 || !appearsIn(excerpt, source))
        continue;

      String sourceType = lower(item, "sourceType");
      String polarity = lower(item, "polarity");
      boolean report = "review".equals(sourceType);
      findings.add(new Finding(
          claim, source.url(), source.title(), excerpt,
          SOURCE_TYPES.contains(sourceType) ? sourceType : "other",
          POLARITIES.contains(polarity) ? polarity : "neutral",
          report,
          "confirmed".equals(lower(item, "entityMatch")),
          retrievedAt));
    }
    if (findings.isEmpty())
      return noData(factor);

    return new FactorResult(factor.id(), EVIDENCE_FOUND, cap(text(node, "summary")), uncertainty(node, findings),
        findings);
  }

  private static Uncertainty uncertainty(JsonNode node, List<Finding> findings) {
    String level = lower(node, "uncertaintyLevel");
    if (!LEVELS.contains(level))
      level = "high";
    String note = cap(text(node, "uncertaintyNote"));

    boolean onlyReports = findings.stream().allMatch(Finding::unverifiedReport);
    boolean unconfirmedEntity = findings.stream().anyMatch(finding -> !finding.entityConfirmed());
    boolean single = findings.size() == 1;
    // Relatos, empresa não confirmada e fonte única nunca sustentam incerteza baixa.
    if ("low".equals(level) && (onlyReports || unconfirmedEntity || single))
      level = "medium";
    if (onlyReports || unconfirmedEntity)
      level = "high";
    return new Uncertainty(level, note);
  }

  /** O trecho precisa estar no título ou no resumo da fonte, ignorando caixa, acentos e espaços repetidos. */
  static boolean appearsIn(String excerpt, DiligenceSource source) {
    String needle = normalize(excerpt);
    return !needle.isEmpty()
        && (normalize(source.snippet()).contains(needle) || normalize(source.title()).contains(needle));
  }

  static String normalize(String value) {
    if (value == null)
      return "";
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .replaceAll("\\s+", " ")
        .strip()
        .toLowerCase(Locale.ROOT);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isTextual() ? cap(value.asText().strip()) : "";
  }

  private static String lower(JsonNode node, String field) {
    return text(node, field).toLowerCase(Locale.ROOT);
  }

  private static String cap(String value) {
    return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
  }
}
