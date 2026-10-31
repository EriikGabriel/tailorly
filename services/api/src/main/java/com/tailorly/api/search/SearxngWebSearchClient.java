package com.tailorly.api.search;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Busca pelo SearXNG, um buscador de código aberto que roda localmente (ver compose.yaml). Não tem
 * custo nem chave de API. A URL vem de {@code tailorly.web-search.searxng-url}; sem ela, nenhuma busca é feita.
 * O formato JSON precisa estar habilitado no SearXNG (ver services/searxng/settings.yml).
 */
@Component
public class SearxngWebSearchClient implements WebSearchClient {
  private final String baseUrl;
  private final RestClient http;

  public SearxngWebSearchClient(@Value("${tailorly.web-search.searxng-url:}") String baseUrl) {
    this.baseUrl = baseUrl == null ? "" : baseUrl.strip().replaceAll("/+$", "");
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public boolean isConfigured() {
    return !baseUrl.isEmpty();
  }

  @Override
  public List<SearchResult> search(String query, int count) {
    if (!isConfigured()) {
      return List.of();
    }
    URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/search")
        .queryParam("q", query).queryParam("format", "json").build().toUri();
    JsonNode body = http.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
    List<SearchResult> results = new ArrayList<>();
    if (body != null) {
      for (JsonNode item : body.path("results")) {
        String url = item.path("url").asText("");
        if (!url.isBlank() && results.size() < count) {
          results.add(new SearchResult(url, item.path("title").asText("")));
        }
      }
    }
    return results;
  }
}
