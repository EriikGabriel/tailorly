package com.tailorly.api.session;

import java.io.IOException;
import java.util.Optional;

import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica requisições com {@code Authorization: Bearer tlr_anon_...}: o token é validado no
 * servidor (hash, expiração, revogação) e a taxa de requisições da sessão é limitada. Tokens
 * sem o prefixo anônimo passam intactos para a validação de JWT das contas.
 */
public class AnonymousSessionAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER = "Bearer ";

  private final AnonymousSessionService sessionService;
  private final FixedWindowRateLimiter requestLimiter;
  private final LocalizedSecurityErrorHandler errorHandler;

  public AnonymousSessionAuthenticationFilter(AnonymousSessionService sessionService,
      FixedWindowRateLimiter requestLimiter, LocalizedSecurityErrorHandler errorHandler) {
    this.sessionService = sessionService;
    this.requestLimiter = requestLimiter;
    this.errorHandler = errorHandler;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String token = bearerToken(request);
    if (!AnonymousSessionTokens.isAnonymousToken(token)) {
      chain.doFilter(request, response);
      return;
    }

    Optional<AnonymousPrincipal> principal = sessionService.authenticate(token);
    if (principal.isEmpty()) {
      errorHandler.invalidSessionToken(request, response);
      return;
    }

    String key = principal.get().sessionId().toString();
    if (!requestLimiter.tryAcquire(key)) {
      errorHandler.tooManyRequests(request, response, requestLimiter.retryAfterSeconds(key));
      return;
    }

    SecurityContextHolder.getContext()
        .setAuthentication(new AnonymousSessionAuthenticationToken(principal.get()));
    chain.doFilter(request, response);
  }

  private static String bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || header.length() <= BEARER.length()
        || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
      return null;
    }
    return header.substring(BEARER.length()).trim();
  }
}
