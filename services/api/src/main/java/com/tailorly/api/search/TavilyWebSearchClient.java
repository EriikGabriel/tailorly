package com.tailorly.api.search;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Busca pela API do Tavily (https://tavily.com), que tem plano gratuito e roda em qualquer host,
 * ao contrário do SearXNG local. Usada em produção, onde não há instância SearXNG. A chave vem de
 * {@code tailorly.web-search.tavily-api-key}; sem ela, nenhuma busca é feita. Instanciado por
 * {@link WebSearchConfiguration}, que escolhe o provedor.
 */
public class TavilyWebSearchClient implements WebSearchClient {
  private static final String ENDPOINT = "https://api.tavily.com/search";
  /** A API aceita de 0 a 20 resultados por busca. */
  private static final int MAX_RESULTS = 20;

  private final String apiKey;
  private final String endpoint;
  private final RestClient http;

  public TavilyWebSearchClient(String apiKey) {
    this(apiKey, ENDPOINT);
  }

  TavilyWebSearchClient(String apiKey, String endpoint) {
    this.apiKey = apiKey == null ? "" : apiKey.strip();
    this.endpoint = endpoint;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public boolean isConfigured() {
    return !apiKey.isEmpty();
  }

  @Override
  public List<SearchResult> search(String query, int count) {
    if (!isConfigured() || count <= 0) {
      return List.of();
    }
    // search_depth "basic" gasta 1 crédito por busca (o plano gratuito tem 1.000 por mês).
    Map<String, Object> request = Map.of(
        "query", query,
        "search_depth", "basic",
        "max_results", Math.min(count, MAX_RESULTS));
    JsonNode body = http.post().uri(endpoint)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(JsonNode.class);
    List<SearchResult> results = new ArrayList<>();
    if (body != null) {
      for (JsonNode item : body.path("results")) {
        String url = item.path("url").asText("");
        if (!url.isBlank() && results.size() < count) {
          results.add(new SearchResult(url, item.path("title").asText(""), item.path("content").asText("")));
        }
      }
    }
    return results;
  }
}
