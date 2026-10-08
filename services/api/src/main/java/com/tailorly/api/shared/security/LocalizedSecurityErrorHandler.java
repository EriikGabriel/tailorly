package com.tailorly.api.shared.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

@Component
@RequiredArgsConstructor
public class LocalizedSecurityErrorHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final MessageSource messageSource;
  private final ObjectMapper objectMapper;
  private final LocaleResolver localeResolver;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Tailorly API\"");
    writeError(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorCodes.Security.AUTHENTICATION_REQUIRED
    );
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException exception
  ) throws IOException, ServletException {
    writeError(request, response, HttpStatus.FORBIDDEN, ErrorCodes.Security.ACCESS_DENIED);
  }

  private void writeError(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String messageCode
  ) throws IOException {
    Locale locale = localeResolver.resolveLocale(request);
    ApiError error = ApiError.builder()
        .timestamp(Instant.now())
        .status(status.value())
        .error(messageSource.getMessage(
            ErrorCodes.HTTP_PREFIX + status.name().toLowerCase(Locale.ROOT),
            null,
            locale
        ))
        .message(messageSource.getMessage(messageCode, null, locale))
        .path(request.getRequestURI())
        .build();

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag());
    response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
    objectMapper.writeValue(response.getOutputStream(), error);
  }
}
