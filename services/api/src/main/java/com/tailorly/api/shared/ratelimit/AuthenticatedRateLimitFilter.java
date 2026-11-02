package com.tailorly.api.shared.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.session.FixedWindowRateLimiter;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite por dono aplicado <em>depois</em> da autenticação. Todas as contas têm um teto geral de
 * requisições; as rotas caras (IA, leitura de páginas externas, processamento de PDF) e a prévia
 * do Typst têm tetos próprios, para que uma conta ou sessão anônima não consuma sozinha o
 * orçamento de IA e a CPU da instância. A sessão anônima já tem o teto geral próprio em
 * {@code AnonymousSessionAuthenticationFilter}, então aqui só recebe os tetos das rotas caras
 * (as cotas de uso do visitante continuam valendo à parte).
 */
public class AuthenticatedRateLimitFilter extends OncePerRequestFilter {
  /** Rotas {@code POST} que chamam IA, baixam páginas externas ou processam arquivos. */
  private static final List<String> HEAVY = List.of(
      "/v1/cv/generate",
      "/v1/cv/generations/*/ats-review",
      "/v1/job-postings",
      "/v1/job-postings/extract",
      "/v1/job-postings/*/versions/*/analysis",
      "/v1/due-diligence",
      "/v1/cv/evidence/snapshots/*/collect",
      "/v1/base-documents/upload",
      "/v1/extraction-runs");
  private static final String PREVIEW = "/v1/cv/generations/*/preview";

  private final AntPathMatcher matcher = new AntPathMatcher();
  private final FixedWindowRateLimiter accountLimiter;
  private final FixedWindowRateLimiter heavyLimiter;
  private final FixedWindowRateLimiter previewLimiter;
  private final LocalizedSecurityErrorHandler errorHandler;

  public AuthenticatedRateLimitFilter(RateLimitProperties properties,
      LocalizedSecurityErrorHandler errorHandler) {
    this.accountLimiter = new FixedWindowRateLimiter(properties.accountRequestsPerMinute(), Duration.ofMinutes(1));
    this.heavyLimiter = new FixedWindowRateLimiter(properties.heavyPerMinute(), Duration.ofMinutes(1));
    this.previewLimiter = new FixedWindowRateLimiter(properties.previewPerMinute(), Duration.ofMinutes(1));
    this.errorHandler = errorHandler;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      chain.doFilter(request, response);
      return;
    }

    String key = authentication.getName();
    boolean anonymousSession = authentication instanceof AnonymousSessionAuthenticationToken;
    FixedWindowRateLimiter blocked = null;
    if (!anonymousSession && !accountLimiter.tryAcquire(key)) {
      blocked = accountLimiter;
    } else {
      FixedWindowRateLimiter route = routeLimiter(request);
      if (route != null && !route.tryAcquire(key)) {
        blocked = route;
      }
    }
    if (blocked != null) {
      errorHandler.tooManyRequests(request, response, blocked.retryAfterSeconds(key));
      return;
    }
    chain.doFilter(request, response);
  }

  private FixedWindowRateLimiter routeLimiter(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())) {
      return null;
    }
    String path = request.getRequestURI();
    if (matcher.match(PREVIEW, path)) {
      return previewLimiter;
    }
    return HEAVY.stream().anyMatch(pattern -> matcher.match(pattern, path)) ? heavyLimiter : null;
  }
}
