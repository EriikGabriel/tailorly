package com.tailorly.api.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebSearchConfigurationTest {
  private final WebSearchConfiguration configuration = new WebSearchConfiguration();

  @Test
  void autoPrefersSearxngWhenItsUrlIsSetEvenWithATavilyKey() {
    var client = configuration.webSearchClient("auto", "http://127.0.0.1:8888", "tvly-key");

    assertInstanceOf(SearxngWebSearchClient.class, client);
    assertTrue(client.isConfigured());
  }

  @Test
  void autoFallsBackToTavilyInProductionWhereOnlyTheKeyIsSet() {
    var client = configuration.webSearchClient("auto", "", "tvly-key");

    assertInstanceOf(TavilyWebSearchClient.class, client);
    assertTrue(client.isConfigured());
  }

  @Test
  void autoWithNothingConfiguredDisablesWebSearch() {
    assertFalse(configuration.webSearchClient("auto", "", "").isConfigured());
  }

  @Test
  void explicitProviderIsHonoured() {
    assertInstanceOf(TavilyWebSearchClient.class,
        configuration.webSearchClient("tavily", "http://127.0.0.1:8888", "tvly-key"));
    assertInstanceOf(SearxngWebSearchClient.class,
        configuration.webSearchClient("searxng", "http://127.0.0.1:8888", "tvly-key"));
  }

  @Test
  void unknownProviderFailsFast() {
    assertThrows(IllegalStateException.class, () -> configuration.webSearchClient("bing", "", ""));
  }
}
