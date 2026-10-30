package com.tailorly.api.job.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tailorly.api.ai.AiChatClient;
import com.tailorly.api.aipreferences.AiPreferencesService;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.reader.PastedTextJobExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JobRequirementsAnalyzerTest {
  private static final String TEXT = """
      Descrição
      Venha construir produtos.
      Requisitos e qualificações
      Experiência sólida com Java e Spring Boot.
      Mais de 5 anos com Kubernetes.
      Diferenciais
      Conhecimento em Kafka.
      """;

  private final ObjectMapper mapper = new ObjectMapper();
  private final AiPreferencesService preferences = mock(AiPreferencesService.class);
  private final UUID owner = UUID.randomUUID();
  private HttpServer server;

  @AfterEach
  void stop() {
    if (server != null)
      server.stop(0);
  }

  private JobPostingResponse job() {
    return JobPostingResponse.builder().title("Desenvolvedor Pleno").descriptionText(TEXT).build();
  }

  private JobRequirementsAnalyzer analyzerReturning(String modelContent) throws IOException {
    byte[] body = mapper.writeValueAsBytes(Map.of("choices", List.of(Map.of("message", Map.of("content", modelContent)))));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/chat", exchange -> {
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/chat";
    when(preferences.resolveEffectiveSettings(owner)).thenReturn(
        new EffectiveAiSettings("remote", endpoint, "m", "k", "http://127.0.0.1:1/x", "m", "k"));
    return new JobRequirementsAnalyzer(mapper, new AiChatClient(), preferences);
  }

  @Test
  void keepsOnlyItemsWithVerbatimEvidenceAndKnownTechnologies() throws IOException {
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        ```json
        {"seniority":"mid","seniorityEvidence":"Desenvolvedor Pleno",
         "requirements":[
          {"text":"Java e Spring Boot","importance":"required","category":"technology","minYears":null,
           "technologies":["Java","Spring Boot","Quarkus"],"evidence":"Experiência sólida com Java e Spring Boot."},
          {"text":"Kubernetes há 5+ anos","importance":"required","category":"technology","minYears":5,
           "technologies":["Kubernetes"],"evidence":"Mais de   5 anos com Kubernetes."},
          {"text":"Kafka","importance":"preferred","category":"technology",
           "technologies":["Kafka"],"evidence":"Conhecimento em Kafka."},
          {"text":"Inventado","importance":"required","category":"other","evidence":"Domínio de Rust avançado"},
          {"text":"Sem importância","importance":"maybe","category":"other","evidence":"Venha construir produtos."}
         ],
         "technologies":["Java","Kafka","Rust"]}
        ```
        """);

    JobAnalysis analysis = analyzer.analyze(owner, job()).orElseThrow();

    assertEquals("mid", analysis.seniority());
    assertEquals("Desenvolvedor Pleno", analysis.seniorityEvidence());
    assertEquals(3, analysis.requirements().size());
    assertEquals(List.of("Java", "Spring Boot"), analysis.requirements().get(0).technologies());
    assertEquals(5, analysis.requirements().get(1).minYears());
    assertEquals("Mais de 5 anos com Kubernetes.", analysis.requirements().get(1).evidence());
    assertEquals("preferred", analysis.requirements().get(2).importance());
    assertEquals(List.of("Java", "Kafka"), analysis.technologies());
    assertEquals(JobRequirementsAnalyzer.VERSION, analysis.analyzerVersion());
  }

  @Test
  void dropsSeniorityWithoutEvidenceInTheText() throws IOException {
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        {"seniority":"senior","seniorityEvidence":"Sênior","requirements":[],"technologies":[]}
        """);

    JobAnalysis analysis = analyzer.analyze(owner, job()).orElseThrow();

    assertNull(analysis.seniority());
    assertNull(analysis.seniorityEvidence());
  }

  @Test
  void failsWhenNoRequirementCanBeVerified() throws IOException {
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        {"requirements":[{"text":"x","importance":"required","category":"other","evidence":"não existe no texto"}]}
        """);

    assertThrows(IllegalStateException.class, () -> analyzer.analyze(owner, job()));
  }

  @Test
  void failsOnMalformedResponses() throws IOException {
    assertThrows(IllegalStateException.class, () -> analyzerReturning("não é json").analyze(owner, job()));
    server.stop(0);
    assertThrows(IllegalStateException.class, () -> analyzerReturning("{\"seniority\":null}").analyze(owner, job()));
  }

  @Test
  void skipsTheAiWhenTheOwnerUsesRules() {
    when(preferences.resolveEffectiveSettings(owner)).thenReturn(
        new EffectiveAiSettings("rules", null, null, null, null, null, null));
    AiChatClient chat = mock(AiChatClient.class);

    Optional<JobAnalysis> analysis = new JobRequirementsAnalyzer(mapper, chat, preferences).analyze(owner, job());

    assertTrue(analysis.isEmpty());
    verify(chat, never()).complete(any(), any(), any());
  }

  @Test
  void skipsTheAiWhenThereIsNoText() {
    AiChatClient chat = mock(AiChatClient.class);

    assertTrue(new JobRequirementsAnalyzer(mapper, chat, preferences)
        .analyze(owner, JobPostingResponse.builder().descriptionText(" ").build()).isEmpty());
    verify(preferences, never()).resolveEffectiveSettings(any());
  }

  private static final String PASTED = """
      Requisitos e qualificações

          Experiência com Java 11+ e Spring Framework; 
          Experiência com React.js, incluindo hooks, componentização e integração com APIs REST.
          Experiência com aplicação dos princípios SOLID e domínio de Design Patterns; 
          Conhecimento prático em Clean Code e Clean Architecture/Hexagonal Architecture; 
          Experiência sólida com testes unitários; 
          Conhecimento em TDD (Test-Driven Development); 
          Experiência com banco de dados relacionais como PostgreSQL;
          Vivência com servidores de aplicação Java, especialmente WildFly; 
          Conhecimento ou interesse em tecnologias de Inteligência Artificial: ferramentas, aplicações e conceitos.
      Diferenciais
          Conhecimento em Kafka.
      """;

  private JobPostingResponse pastedJob() {
    return new PastedTextJobExtractor().extract(PASTED, "Dev Backend", null);
  }

  @Test
  void everyItemFoundByTheSectionSplitStaysEvenWhenTheModelOnlyAnnotatesSome() throws IOException {
    // O modelo anota só 3 dos 10 itens, erra "Java 11+" como 11 anos e descarta o item 9.
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        {"seniority":null,"seniorityEvidence":null,
         "items":[
          {"id":1,"keep":true,"category":"technology","minYears":11,"technologies":["Java","Spring Framework","Quarkus"]},
          {"id":2,"keep":true,"category":"technology","technologies":["React.js","REST"]},
          {"id":9,"keep":false}
         ],
         "technologies":["Java","WildFly","Rust"]}
        """);

    JobAnalysis analysis = analyzer.analyze(owner, pastedJob()).orElseThrow();

    // 9 requisitos + 1 desejável - 1 descartado pelo modelo.
    assertEquals(9, analysis.requirements().size());
    assertEquals("Experiência com Java 11+ e Spring Framework", analysis.requirements().get(0).text());
    assertEquals("Experiência com Java 11+ e Spring Framework;", analysis.requirements().get(0).evidence());
    assertEquals(List.of("Java", "Spring Framework"), analysis.requirements().get(0).technologies());
    assertNull(analysis.requirements().get(0).minYears(), "versão do Java não é tempo de experiência");
    assertEquals(List.of("React.js", "REST"), analysis.requirements().get(1).technologies());

    // Sem anotação do modelo, o item continua, sem categoria inventada.
    var tdd = analysis.requirements().stream().filter(r -> r.text().contains("TDD")).findFirst().orElseThrow();
    assertEquals("required", tdd.importance());
    assertEquals("other", tdd.category());
    assertEquals(List.of(), tdd.technologies());
    assertTrue(analysis.requirements().stream().noneMatch(r -> r.text().contains("Inteligência Artificial")));

    // A importância vem da seção; o desejável fica desejável.
    var kafka = analysis.requirements().getLast();
    assertEquals("Conhecimento em Kafka.", kafka.text());
    assertEquals("preferred", kafka.importance());

    // Tecnologias que não aparecem no texto são descartadas (Quarkus, Rust).
    assertEquals(List.of("Java", "Spring Framework", "React.js", "REST", "WildFly"), analysis.technologies());
  }

  @Test
  void aLineWithDesirableWordingIsPreferredEvenInsideRequirements() throws IOException {
    JobPostingResponse job = new PastedTextJobExtractor().extract(
        "Dev\nRequisitos\nExperiência com Java.\nDesejável conhecimento em Docker.", null, null);
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        {"items":[{"id":1,"technologies":["Java"]},{"id":2,"technologies":["Docker"]}],"technologies":[]}
        """);

    JobAnalysis analysis = analyzer.analyze(owner, job).orElseThrow();

    assertEquals("required", analysis.requirements().get(0).importance());
    assertEquals("preferred", analysis.requirements().get(1).importance());
  }

  @Test
  void annotationModeFailsWhenTheModelReturnsNoItems() throws IOException {
    assertThrows(IllegalStateException.class,
        () -> analyzerReturning("{\"seniority\":null}").analyze(owner, pastedJob()));
  }

  @Test
  void freeFormModeAcceptsPortugueseImportanceAndRejectsVersionsAsYears() throws IOException {
    JobRequirementsAnalyzer analyzer = analyzerReturning("""
        {"requirements":[
          {"text":"Java 11+","importance":"Obrigatório","category":"technology","minYears":11,
           "technologies":["Java"],"evidence":"Experiência sólida com Java e Spring Boot."},
          {"text":"Kubernetes","importance":"desejável","category":"technology","minYears":5,
           "technologies":["Kubernetes"],"evidence":"Mais de 5 anos com Kubernetes."}],
         "technologies":[]}
        """);

    JobAnalysis analysis = analyzer.analyze(owner, job()).orElseThrow();

    assertEquals("required", analysis.requirements().get(0).importance());
    assertNull(analysis.requirements().get(0).minYears());
    assertEquals("preferred", analysis.requirements().get(1).importance());
    assertEquals(5, analysis.requirements().get(1).minYears());
  }
}
