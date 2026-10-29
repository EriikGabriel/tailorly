package com.tailorly.api.extraction.interpreting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AiCurriculumInterpreterTest {
  private static final UUID PAGE_ID = UUID.randomUUID();
  private static final List<ExtractedTextPage> PAGES = List.of(new ExtractedTextPage(PAGE_ID, 1, "pdf-text",
      "Erik Gabriel\nerik@example.com | (11) 98765-4321\nTech Lead\nFinTech Solutions Brasil\n"));

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null)
      server.stop(0);
  }

  @Test
  void acceptsFencedJsonStringPageNumbersAndWhitespaceDifferences() throws IOException {
    String content = """
        ```json
        {"facts": [
          {"category": "identity", "fieldKey": "phone", "extractedValue": "(11) 98765-4321",
           "pageNumber": "1", "sourceExcerpt": "(11) 98765-4321"},
          {"category": "experience", "fieldKey": "company", "extractedValue": "FinTech Solutions Brasil",
           "pageNumber": 1, "sourceExcerpt": "Tech Lead FinTech Solutions Brasil"}
        ]}
        ```
        """;

    List<FactProposal> proposals = interpret(content);

    assertEquals(2, proposals.size());
    assertEquals("(11) 98765-4321", proposals.get(0).sourceExcerpt());
    // O trecho persistido é o texto original da página, com a quebra de linha, e não a versão do modelo.
    assertEquals("Tech Lead\nFinTech Solutions Brasil", proposals.get(1).sourceExcerpt());
    assertEquals(PAGE_ID, proposals.get(1).extractedPageId());
  }

  @Test
  void discardsFactsWhoseExcerptIsNotInThePage() throws IOException {
    String content = """
        {"facts": [
          {"category": "identity", "fieldKey": "email", "extractedValue": "erik@example.com",
           "pageNumber": 1, "sourceExcerpt": "erik@example.com"},
          {"category": "skills", "fieldKey": "skill", "extractedValue": "Kubernetes",
           "pageNumber": 1, "sourceExcerpt": "Kubernetes expert"}
        ]}
        """;

    List<FactProposal> proposals = interpret(content);

    assertEquals(1, proposals.size());
    assertEquals("email", proposals.get(0).fieldKey());
  }

  @Test
  void keepsOnlySchemaFieldsAndNormalizesItemIndexes() throws IOException {
    String content = """
        {"facts": [
          {"category": "experience", "fieldKey": "company", "extractedValue": "FinTech Solutions Brasil",
           "pageNumber": 1, "sourceExcerpt": "FinTech Solutions Brasil"},
          {"category": "experience", "fieldKey": "1:role", "extractedValue": "Tech Lead",
           "pageNumber": 1, "sourceExcerpt": "Tech Lead"},
          {"category": "experience", "fieldKey": "salary", "extractedValue": "Tech Lead",
           "pageNumber": 1, "sourceExcerpt": "Tech Lead"},
          {"category": "hobbies", "fieldKey": "name", "extractedValue": "Erik Gabriel",
           "pageNumber": 1, "sourceExcerpt": "Erik Gabriel"},
          {"category": "Identity", "fieldKey": "name", "extractedValue": "Erik Gabriel",
           "pageNumber": 1, "sourceExcerpt": "Erik Gabriel"}
        ]}
        """;

    List<FactProposal> proposals = interpret(content);

    assertEquals(List.of("0:company", "1:role", "name"),
        proposals.stream().map(FactProposal::fieldKey).toList());
    assertEquals("identity", proposals.get(2).category());
  }

  @Test
  void acceptsTheVolunteeringCategoryWithItsOwnFields() throws IOException {
    String content = """
        {"facts": [
          {"category": "volunteering", "fieldKey": "organization", "extractedValue": "FinTech Solutions Brasil",
           "pageNumber": 1, "sourceExcerpt": "FinTech Solutions Brasil"},
          {"category": "volunteering", "fieldKey": "0:role", "extractedValue": "Tech Lead",
           "pageNumber": 1, "sourceExcerpt": "Tech Lead"},
          {"category": "volunteering", "fieldKey": "company", "extractedValue": "FinTech Solutions Brasil",
           "pageNumber": 1, "sourceExcerpt": "FinTech Solutions Brasil"}
        ]}
        """;

    List<FactProposal> proposals = interpret(content);

    // "company" não existe em voluntariado (o campo é "organization"), então é descartado.
    assertEquals(List.of("0:organization", "0:role"),
        proposals.stream().map(FactProposal::fieldKey).toList());
    assertEquals("volunteering", proposals.get(0).category());
  }

  @Test
  void failsWhenNoFactHasVerifiableEvidence() throws IOException {
    String content = """
        {"facts": [
          {"category": "skills", "fieldKey": "skill", "extractedValue": "Kubernetes",
           "pageNumber": 1, "sourceExcerpt": "Kubernetes expert"}
        ]}
        """;

    assertThrows(IllegalStateException.class, () -> interpret(content));
  }

  private List<FactProposal> interpret(String modelContent) throws IOException {
    byte[] body = objectMapper.writeValueAsBytes(
        java.util.Map.of("choices", List.of(java.util.Map.of("message", java.util.Map.of("content", modelContent)))));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/chat/completions", exchange -> {
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();

    String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    EffectiveAiSettings settings = new EffectiveAiSettings("remote", endpoint, "test-model", "key",
        "http://127.0.0.1:1/unused", "unused", "unused");

    return new AiCurriculumInterpreter(objectMapper, new AiChatClient()).interpret(PAGES, settings);
  }
}
