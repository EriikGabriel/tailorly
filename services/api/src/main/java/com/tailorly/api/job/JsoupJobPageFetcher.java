package com.tailorly.api.job;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.UnprocessableRequestException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Baixa páginas de vaga com Jsoup. Os redirecionamentos são seguidos manualmente para que cada
 * destino passe por {@link JobUrlPolicy}, e o corpo é limitado para não carregar arquivos grandes.
 */
@Component
@RequiredArgsConstructor
public class JsoupJobPageFetcher implements JobPageFetcher {
  private static final Logger log = LoggerFactory.getLogger(JsoupJobPageFetcher.class);

  private static final int MAX_REDIRECTS = 5;
  private static final int TIMEOUT_MILLIS = 10_000;
  private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
  private static final String USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

  private final JobUrlPolicy urlPolicy;

  @Override
  public FetchedPage fetch(String url) {
    URI current = urlPolicy.validate(url);

    for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
      Connection.Response response = request(current);
      int status = response.statusCode();

      if (status >= 300 && status < 400) {
        if (hop == MAX_REDIRECTS)
          break;
        String location = response.header("Location");
        if (location == null || location.isBlank())
          throw unavailable(current, "redirect without Location", null);
        // Revalida o destino: um site público pode redirecionar para um endereço interno.
        try {
          current = urlPolicy.validate(current.resolve(location.trim()).toString());
        } catch (IllegalArgumentException malformedLocation) {
          throw unavailable(current, "malformed redirect Location", malformedLocation);
        }
        continue;
      }

      if (status >= 400)
        throw unavailable(current, "HTTP " + status, null);

      String contentType = response.contentType();
      if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("html"))
        throw new UnprocessableRequestException(ErrorCodes.Job.NOT_HTML);

      try {
        return new FetchedPage(response.parse(), current.toString());
      } catch (IOException exception) {
        throw unavailable(current, "could not parse body", exception);
      }
    }
    throw unavailable(current, "too many redirects", null);
  }

  private Connection.Response request(URI uri) {
    try {
      return Jsoup.connect(uri.toString())
          .userAgent(USER_AGENT)
          .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
          .timeout(TIMEOUT_MILLIS)
          .maxBodySize(MAX_BODY_BYTES)
          .followRedirects(false)
          .ignoreHttpErrors(true)
          // O tipo é conferido depois do status: erros HTTP costumam vir como JSON e não são "não-HTML".
          .ignoreContentType(true)
          .execute();
    } catch (IOException exception) {
      throw unavailable(uri, exception.getClass().getSimpleName(), exception);
    }
  }

  private static UnprocessableRequestException unavailable(URI uri, String reason, Exception cause) {
    log.info("Job page unavailable host={} reason={}", uri.getHost() == null ? "?" : uri.getHost().toLowerCase(Locale.ROOT), reason);
    UnprocessableRequestException exception = new UnprocessableRequestException(ErrorCodes.Job.PAGE_UNAVAILABLE);
    if (cause != null)
      exception.initCause(cause);
    return exception;
  }
}
