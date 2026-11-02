package com.tailorly.api.shared.ratelimit;

import java.io.IOException;
import java.time.Duration;

import com.tailorly.api.session.FixedWindowRateLimiter;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite por IP aplicado <em>antes</em> da autenticação: protege login, refresh, logout e
 * cadastro contra força bruta e criação em massa de contas, e impõe um teto geral de requisições
 * por IP para que tokens inválidos não sirvam para martelar o banco. O health check fica de
 * fora, porque o orquestrador o consulta com frequência.
 */
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {
  private final FixedWindowRateLimiter ipLimiter;
  private final FixedWindowRateLimiter authLimiter;
  private final FixedWindowRateLimiter registerLimiter;
  private final LocalizedSecurityErrorHandler errorHandler;

  public PublicEndpointRateLimitFilter(RateLimitProperties properties,
      LocalizedSecurityErrorHandler errorHandler) {
    this.ipLimiter = new FixedWindowRateLimiter(properties.ipRequestsPerMinute(), Duration.ofMinutes(1));
    this.authLimiter = new FixedWindowRateLimiter(properties.authPerMinute(), Duration.ofMinutes(1));
    this.registerLimiter = new FixedWindowRateLimiter(properties.registerPerHour(), Duration.ofHours(1));
    this.errorHandler = errorHandler;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String ip = request.getRemoteAddr();
    FixedWindowRateLimiter specific = specificLimiter(request);
    FixedWindowRateLimiter blocked = null;
    if (!ipLimiter.tryAcquire(ip)) {
      blocked = ipLimiter;
    } else if (specific != null && !specific.tryAcquire(ip)) {
      blocked = specific;
    }
    if (blocked != null) {
      errorHandler.tooManyRequests(request, response, blocked.retryAfterSeconds(ip));
      return;
    }
    chain.doFilter(request, response);
  }

  private FixedWindowRateLimiter specificLimiter(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())) {
      return null;
    }
    return switch (request.getRequestURI()) {
      case "/v1/auth/login", "/v1/auth/refresh", "/v1/auth/logout" -> authLimiter;
      case "/v1/auth/register", "/v1/users" -> registerLimiter;
      default -> null;
    };
  }
}
