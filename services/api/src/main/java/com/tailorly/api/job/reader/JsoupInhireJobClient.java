package com.tailorly.api.job.reader;

import java.io.IOException;
import java.util.Optional;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Consulta {@code https://api.inhire.app/job-posts/public/pages/{jobId}}, o mesmo endpoint que a página
 * pública usa. O host é fixo e o tenant e o ID já chegam validados por {@link InhireJobExtractor}; por isso
 * não há como apontar a requisição para outro destino (SSRF).
 */
@Component
@RequiredArgsConstructor
public class JsoupInhireJobClient implements InhireJobClient {
  private static final Logger log = LoggerFactory.getLogger(JsoupInhireJobClient.class);
  private static final String ENDPOINT = "https://api.inhire.app/job-posts/public/pages/";
  private static final int TIMEOUT_MILLIS = 10_000;
  private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

  private final ObjectMapper objectMapper;

  @Override
  public Optional<JsonNode> fetch(String tenant, String jobId) {
    Connection.Response response;
    try {
      response = Jsoup.connect(ENDPOINT + jobId)
          .header("x-tenant", tenant)
          .header("Accept", "application/json")
          .timeout(TIMEOUT_MILLIS)
          .maxBodySize(MAX_BODY_BYTES)
          .followRedirects(false)
          .ignoreHttpErrors(true)
          .ignoreContentType(true)
          .execute();
    } catch (IOException exception) {
      throw unavailable("network " + exception.getClass().getSimpleName(), exception);
    }

    int status = response.statusCode();
    if (status == 404 || status == 400)
      return Optional.empty();
    if (status != 200)
      throw unavailable("HTTP " + status, null);
    try {
      JsonNode body = objectMapper.readTree(response.body());
      return body.isObject() ? Optional.of(body) : Optional.empty();
    } catch (JacksonException exception) {
      throw unavailable("invalid JSON", exception);
    }
  }

  private static UnprocessableRequestException unavailable(String reason, Exception cause) {
    log.info("InHire job unavailable reason={}", reason);
    UnprocessableRequestException exception = new UnprocessableRequestException(ErrorCodes.Job.PAGE_UNAVAILABLE);
    if (cause != null)
      exception.initCause(cause);
    return exception;
  }
}
