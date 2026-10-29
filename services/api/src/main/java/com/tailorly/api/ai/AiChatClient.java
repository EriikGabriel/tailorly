package com.tailorly.api.ai;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailorly.api.aipreferences.EffectiveAiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP único para provedores compatíveis com Chat Completions da OpenAI. Resolve o
 * provider efetivo ({@code remote}, {@code ollama-only} ou {@code remote-with-ollama-fallback}) e
 * deixa o prompt e a validação da resposta com cada chamador.
 */
@Component
public class AiChatClient {
  private static final String REMOTE = "Remote provider";
  private static final String OLLAMA = "Ollama";

  private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);

  // Força HTTP/1.1: o HttpClient do JDK, em preferência padrão, tenta upgrade h2c (HTTP/2 em
  // texto puro) mesmo contra endpoints http:// sem TLS; servidores locais comuns (gateways de
  // LLM, Ollama, proxies Node) não respondem a esse upgrade e fecham a conexão sem enviar
  // nenhum byte, o que aparece como EOFException/"header parser received no bytes" no cliente.
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  // Gateways de LLM podem encadear vários provedores antes de responder; sem limite, a extração
  // ficaria bloqueada e o fallback para o Ollama local nunca começaria.
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

  private final RestClient restClient = RestClient.builder()
      .requestFactory(readTimeoutFactory())
      .build();

  public record ModelConfiguration(String provider, String endpoint, String model, String apiKey) {
  }

  /**
   * Executa {@code attempt} com o provedor definido em {@code settings}. No modo
   * {@code remote-with-ollama-fallback}, qualquer falha do remoto (inclusive de validação da
   * resposta dentro de {@code attempt}) tenta o Ollama local antes de desistir (RNF07).
   */
  public <T> T run(EffectiveAiSettings settings, String task, Function<ModelConfiguration, T> attempt) {
    if ("ollama-only".equals(settings.provider()))
      return attempt.apply(ollamaConfiguration(settings));

    if ("remote".equals(settings.provider()))
      return attempt.apply(remoteConfiguration(settings));

    ModelConfiguration remoteConfig = remoteConfiguration(settings);
    try {
      return attempt.apply(remoteConfig);
    } catch (RuntimeException remoteFailure) {
      log.warn("Remote provider {} failed at endpoint {} (model {}); trying local Ollama fallback",
          task, remoteConfig.endpoint(), remoteConfig.model(), remoteFailure);
      ModelConfiguration ollamaConfig = ollamaConfiguration(settings);
      try {
        T result = attempt.apply(ollamaConfig);
        log.info("Ollama fallback succeeded for {} at endpoint {} (model {})",
            task, ollamaConfig.endpoint(), ollamaConfig.model());
        return result;
      } catch (RuntimeException ollamaFailure) {
        log.warn("Ollama fallback for {} also failed at endpoint {} (model {})",
            task, ollamaConfig.endpoint(), ollamaConfig.model(), ollamaFailure);
        IllegalStateException combinedFailure = new IllegalStateException(
            "Remote provider " + task + " and local Ollama fallback both failed", ollamaFailure);

        combinedFailure.addSuppressed(remoteFailure);
        throw combinedFailure;
      }
    }
  }

  /** Envia o prompt e devolve o texto gerado; a resposta ainda deve ser validada por quem chama. */
  public String complete(ModelConfiguration configuration, String systemPrompt, String userPrompt) {
    validateConfiguration(configuration);

    // Solicita JSON para simplificar a leitura; ainda assim a resposta será validada localmente.
    Map<String, Object> request = Map.of(
        "model", configuration.model(),
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)));
    try {
      JsonNode response = postWithRetry(configuration, request);

      // A API encapsula o conteúdo gerado em choices[0].message.content.
      JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");

      if (content == null || !content.isTextual()) {
        throw new IllegalStateException(configuration.provider() + " response did not contain message content");
      }

      return content.asText();
    } catch (RestClientException exception) {
      throw new IllegalStateException(configuration.provider() + " request failed", exception);
    }
  }

  // Alguns modelos ignoram response_format e envolvem o JSON em cercas markdown (```json ... ```).
  public static String stripCodeFence(String response) {
    String trimmed = response.trim();
    if (!trimmed.startsWith("```"))
      return trimmed;

    int firstLineEnd = trimmed.indexOf('\n');
    int closingFence = trimmed.lastIndexOf("```");
    if (firstLineEnd < 0 || closingFence <= firstLineEnd)
      return trimmed;

    return trimmed.substring(firstLineEnd + 1, closingFence).trim();
  }

  private static ModelConfiguration remoteConfiguration(EffectiveAiSettings settings) {
    return new ModelConfiguration(REMOTE, settings.remoteEndpoint(), settings.remoteModel(), settings.remoteApiKey());
  }

  private static ModelConfiguration ollamaConfiguration(EffectiveAiSettings settings) {
    return new ModelConfiguration(OLLAMA, settings.ollamaEndpoint(), settings.ollamaModel(), settings.ollamaApiKey());
  }

  private static void validateConfiguration(ModelConfiguration configuration) {
    if (configuration.model() == null || configuration.model().isBlank()) {
      throw new IllegalStateException(configuration.provider() + " model is not configured");
    }
    if (configuration.apiKey() == null || configuration.apiKey().isBlank()) {
      throw new IllegalStateException(configuration.provider() + " API key is not configured");
    }
  }

  /**
   * O {@code java.net.http.HttpClient} usado pelo {@link RestClient} reaproveita conexões
   * mantidas em pool; se o servidor remoto fechar uma conexão ociosa entre duas chamadas
   * agendadas, a tentativa seguinte falha com EOF antes de qualquer byte de resposta
   * ({@link ResourceAccessException}). Repetir uma única vez com uma conexão nova resolve essa
   * falha transitória sem mascarar erros reais do provedor (HTTP 4xx/5xx chegam normalmente, sem
   * passar por aqui).
   */
  private static JdkClientHttpRequestFactory readTimeoutFactory() {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(CONNECT_TIMEOUT)
        .build());
    factory.setReadTimeout(READ_TIMEOUT);
    return factory;
  }

  private JsonNode postWithRetry(ModelConfiguration configuration, Map<String, Object> request) {
    try {
      return postOnce(configuration, request);
    } catch (ResourceAccessException staleConnection) {
      // Repetir após um timeout só dobraria a espera sem chance real de sucesso.
      if (staleConnection.getCause() instanceof HttpTimeoutException)
        throw staleConnection;
      log.warn("{} request hit a stale connection (I/O error before any response byte); retrying once",
          configuration.provider(), staleConnection);
      return postOnce(configuration, request);
    }
  }

  private JsonNode postOnce(ModelConfiguration configuration, Map<String, Object> request) {
    return restClient.post()
        .uri(configuration.endpoint())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + configuration.apiKey())
        .body(request)
        .retrieve()
        .body(JsonNode.class);
  }
}
