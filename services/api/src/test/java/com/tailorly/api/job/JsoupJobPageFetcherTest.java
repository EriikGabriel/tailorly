package com.tailorly.api.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsoupJobPageFetcherTest {
  private HttpServer server;
  private String base;
  private JobUrlPolicy policy;
  private JsoupJobPageFetcher fetcher;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    // O servidor de teste fica em loopback, que a política real bloquearia; aqui só se verifica o fetcher.
    policy = mock(JobUrlPolicy.class);
    when(policy.validate(anyString())).thenAnswer(invocation -> URI.create(invocation.getArgument(0)));
    fetcher = new JsoupJobPageFetcher(policy);
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private void respond(String path, int status, String contentType, String body, String location) {
    server.createContext(path, exchange -> {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", contentType);
      if (location != null)
        exchange.getResponseHeaders().add("Location", location);
      exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
      if (bytes.length > 0)
        exchange.getResponseBody().write(bytes);
      exchange.close();
    });
  }

  @Test
  void downloadsAndParsesHtml() {
    respond("/vaga", 200, "text/html; charset=utf-8", "<html><body><h1>Dev</h1></body></html>", null);

    FetchedPage page = fetcher.fetch(base + "/vaga");

    assertEquals("Dev", page.document().selectFirst("h1").text());
    assertEquals(base + "/vaga", page.finalUrl());
  }

  @Test
  void followsRedirectsAndValidatesEveryHop() {
    respond("/a", 302, "text/html", "", "/b");
    respond("/b", 301, "text/html", "", base + "/c");
    respond("/c", 200, "text/html", "<html><body>fim</body></html>", null);

    FetchedPage page = fetcher.fetch(base + "/a");

    assertEquals(base + "/c", page.finalUrl());
    verify(policy).validate(base + "/a");
    verify(policy).validate(base + "/b");
    verify(policy).validate(base + "/c");
  }

  @Test
  void stopsWhenARedirectTargetIsNotAllowed() {
    respond("/a", 302, "text/html", "", "http://169.254.169.254/latest/meta-data/");
    when(policy.validate("http://169.254.169.254/latest/meta-data/"))
        .thenThrow(new BadRequestException(ErrorCodes.Job.URL_NOT_ALLOWED));

    BadRequestException error = assertThrows(BadRequestException.class, () -> fetcher.fetch(base + "/a"));

    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, error.getMessage());
  }

  @Test
  void givesUpOnRedirectLoops() {
    respond("/loop", 302, "text/html", "", "/loop");

    UnprocessableRequestException error =
        assertThrows(UnprocessableRequestException.class, () -> fetcher.fetch(base + "/loop"));

    assertEquals(ErrorCodes.Job.PAGE_UNAVAILABLE, error.getMessage());
    verify(policy, times(6)).validate(base + "/loop");
  }

  @Test
  void reportsHttpErrorsAsUnavailable() {
    respond("/gone", 404, "text/html", "<html>nada</html>", null);
    respond("/blocked", 403, "text/html", "<html>bot</html>", null);
    respond("/api-error", 404, "application/json", "{\"error\":\"not found\"}", null);

    for (String path : new String[] {"/gone", "/blocked", "/api-error"}) {
      UnprocessableRequestException error =
          assertThrows(UnprocessableRequestException.class, () -> fetcher.fetch(base + path));
      assertEquals(ErrorCodes.Job.PAGE_UNAVAILABLE, error.getMessage(), path);
    }
  }

  @Test
  void rejectsNonHtmlContent() {
    respond("/doc.pdf", 200, "application/pdf", "%PDF-1.7", null);

    UnprocessableRequestException error =
        assertThrows(UnprocessableRequestException.class, () -> fetcher.fetch(base + "/doc.pdf"));

    assertEquals(ErrorCodes.Job.NOT_HTML, error.getMessage());
  }

  @Test
  void truncatesOversizedBodiesInsteadOfLoadingThemWhole() {
    String filler = "x".repeat(3 * 1024 * 1024);
    respond("/big", 200, "text/html", "<html><body><p>" + filler + "</p></body></html>", null);

    FetchedPage page = fetcher.fetch(base + "/big");

    assertTrue(page.document().text().length() < 2 * 1024 * 1024 + 100);
  }

  @Test
  void connectionFailuresBecomeUnavailable() {
    server.stop(0);

    UnprocessableRequestException error =
        assertThrows(UnprocessableRequestException.class, () -> fetcher.fetch(base + "/qualquer"));

    assertEquals(ErrorCodes.Job.PAGE_UNAVAILABLE, error.getMessage());
  }
}
