package com.tailorly.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TavilyWebSearchClientTest {
  private HttpServer server;
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/search", exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] response = ("{\"results\":["
          + "{\"url\":\"https://a.example/x\",\"title\":\"A\",\"content\":\"resumo A\"},"
          + "{\"url\":\"\",\"title\":\"sem url\",\"content\":\"\"},"
          + "{\"url\":\"https://b.example/y\",\"title\":\"B\",\"content\":\"resumo B\"},"
          + "{\"url\":\"https://c.example/z\",\"title\":\"C\",\"content\":\"resumo C\"}]}")
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private String endpoint() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/search";
  }

  @Test
  void sendsBearerKeyAndMapsResultsSkippingBlankUrlsAndHonouringCount() {
    var client = new TavilyWebSearchClient(" tvly-secret ", endpoint());

    List<WebSearchClient.SearchResult> results = client.search("projeto tailorly", 2);

    assertEquals("Bearer tvly-secret", authorization.get());
    assertTrue(requestBody.get().contains("\"query\":\"projeto tailorly\""));
    assertTrue(requestBody.get().contains("\"max_results\":2"));
    assertEquals(2, results.size());
    assertEquals(new WebSearchClient.SearchResult("https://a.example/x", "A", "resumo A"), results.get(0));
    assertEquals("https://b.example/y", results.get(1).url());
  }

  @Test
  void withoutKeyItIsNotConfiguredAndNeverCallsTheApi() {
    var client = new TavilyWebSearchClient("  ", endpoint());

    assertFalse(client.isConfigured());
    assertTrue(client.search("qualquer", 3).isEmpty());
    assertEquals(null, authorization.get());
  }
}
