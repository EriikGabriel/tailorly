package com.tailorly.api.diligence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tools.jackson.databind.ObjectMapper;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import com.tailorly.api.diligence.dto.DueDiligenceReport;
import com.tailorly.api.diligence.dto.DueDiligenceReport.Entity;
import com.tailorly.api.diligence.dto.DueDiligenceReport.FactorResult;
import com.tailorly.api.search.WebSearchClient;
import com.tailorly.api.search.WebSearchClient.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Busca fontes públicas para cada um dos onze fatores e pede à IA do owner que extraia conclusões
 * rastreáveis dessas fontes. A IA só vê título e resumo dos resultados (dado não confiável); não há acesso a
 * fontes privadas, que dependem de integração e acesso autorizado ainda inexistentes. Não toca no Ground Truth.
 */
@Component
public class DueDiligenceAnalyzer {
  public static final String VERSION = "due-diligence-1";

  private static final Logger log = LoggerFactory.getLogger(DueDiligenceAnalyzer.class);
  private static final int RESULTS_PER_FACTOR = 4;
  private static final int MAX_SNIPPET = 500;

  private final WebSearchClient search;
  private final AiChatClient chat;
  private final AiPreferencesService preferences;
  private final DueDiligenceParser parser;

  public DueDiligenceAnalyzer(ObjectMapper objectMapper, WebSearchClient search, AiChatClient chat,
      AiPreferencesService preferences) {
    this.search = search;
    this.chat = chat;
    this.preferences = preferences;
    this.parser = new DueDiligenceParser(objectMapper);
  }

  public boolean isSearchConfigured() {
    return search.isConfigured();
  }

  public boolean isAiAvailable(UUID ownerId) {
    return !"rules".equals(preferences.resolveEffectiveSettings(ownerId).provider());
  }

  /** O resultado das buscas; {@code unavailable} são os fatores cuja busca falhou. */
  record Gathered(Map<DiligenceFactor, List<DiligenceSource>> sources, Set<DiligenceFactor> unavailable) {
    boolean hasSources() {
      return sources.values().stream().anyMatch(list -> !list.isEmpty());
    }
  }

  /** Uma busca por fator, em paralelo. Lança se todas falharem (busca fora do ar). */
  Gathered gather(String company) {
    Map<DiligenceFactor, List<SearchResult>> raw = new EnumMap<>(DiligenceFactor.class);
    Set<DiligenceFactor> unavailable = EnumSet.noneOf(DiligenceFactor.class);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Map<DiligenceFactor, CompletableFuture<List<SearchResult>>> futures = new EnumMap<>(DiligenceFactor.class);
      for (DiligenceFactor factor : DiligenceFactor.all())
        futures.put(factor, CompletableFuture.supplyAsync(
            () -> search.search(factor.query(company), RESULTS_PER_FACTOR), executor));
      futures.forEach((factor, future) -> {
        try {
          raw.put(factor, future.join());
        } catch (RuntimeException failure) {
          log.info("Due diligence search failed for factor {}: {}", factor.id(), failure.getClass().getSimpleName());
          unavailable.add(factor);
        }
      });
    }
    if (unavailable.size() == DiligenceFactor.values().length)
      throw new IllegalStateException("All due diligence searches failed");

    // Uma mesma URL mantém o mesmo número em todos os fatores.
    Map<String, Integer> ids = new java.util.LinkedHashMap<>();
    Map<DiligenceFactor, List<DiligenceSource>> sources = new EnumMap<>(DiligenceFactor.class);
    for (DiligenceFactor factor : DiligenceFactor.all()) {
      List<DiligenceSource> list = new ArrayList<>();
      Set<String> seen = new LinkedHashSet<>();
      for (SearchResult result : raw.getOrDefault(factor, List.of())) {
        if (result.url() == null || result.url().isBlank() || !seen.add(result.url()))
          continue;
        int id = ids.computeIfAbsent(result.url(), url -> ids.size() + 1);
        list.add(new DiligenceSource(id, result.url(), clip(result.title(), 200), clip(result.snippet(), MAX_SNIPPET)));
      }
      sources.put(factor, list);
    }
    return new Gathered(sources, unavailable);
  }

  /** Chama a IA (apenas se houver fontes) e devolve o relatório com os onze fatores. */
  DueDiligenceReport analyze(UUID ownerId, Entity entity, Gathered gathered, Instant consultedAt) {
    List<FactorResult> factors;
    if (!gathered.hasSources()) {
      factors = new ArrayList<>();
      for (DiligenceFactor factor : DiligenceFactor.all())
        factors.add(gathered.unavailable().contains(factor)
            ? new FactorResult(factor.id(), DueDiligenceParser.SOURCE_UNAVAILABLE, "",
                new DueDiligenceReport.Uncertainty("high", ""), List.of())
            : DueDiligenceParser.noData(factor));
    } else {
      EffectiveAiSettings settings = preferences.resolveEffectiveSettings(ownerId);
      String prompt = DueDiligencePrompts.build(entity, gathered.sources());
      factors = chat.run(settings, "due diligence", configuration -> parser.parse(
          chat.complete(configuration, DueDiligencePrompts.SYSTEM, prompt),
          gathered.sources(), gathered.unavailable(), consultedAt));
    }
    return new DueDiligenceReport(entity, consultedAt, List.of("public_web_search"), false, VERSION, factors);
  }

  private static String clip(String value, int max) {
    if (value == null)
      return "";
    String clean = value.replaceAll("\\s+", " ").strip();
    return clean.length() <= max ? clean : clean.substring(0, max);
  }
}
