package com.tailorly.api.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe o provedor de busca web. Com {@code tailorly.web-search.provider=auto} (padrão), o
 * SearXNG vale quando {@code searxng-url} está definida (perfil {@code local}) e o Tavily quando
 * só há chave de API (produção). {@code searxng} e {@code tavily} forçam um dos dois; sem a
 * configuração do escolhido, nenhuma busca é feita.
 */
@Configuration
public class WebSearchConfiguration {

  @Bean
  public WebSearchClient webSearchClient(
      @Value("${tailorly.web-search.provider:auto}") String provider,
      @Value("${tailorly.web-search.searxng-url:}") String searxngUrl,
      @Value("${tailorly.web-search.tavily-api-key:}") String tavilyApiKey
  ) {
    String choice = provider.strip().toLowerCase();
    return switch (choice) {
      case "searxng" -> new SearxngWebSearchClient(searxngUrl);
      case "tavily" -> new TavilyWebSearchClient(tavilyApiKey);
      case "auto", "" -> searxngUrl != null && !searxngUrl.isBlank()
          ? new SearxngWebSearchClient(searxngUrl)
          : new TavilyWebSearchClient(tavilyApiKey);
      default -> throw new IllegalStateException(
          "tailorly.web-search.provider must be auto, searxng or tavily, got: " + provider);
    };
  }
}
