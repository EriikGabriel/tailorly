package com.tailorly.api.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.session.AnonymousPrincipal;
import com.tailorly.api.session.AnonymousSessionAuthenticationToken;
import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFiltersTest {
  private final RateLimitProperties limits = new RateLimitProperties(
      true, 8, 2, 1, 3, 20, Duration.ofMinutes(15), 5, 2, 3);
  private LocalizedSecurityErrorHandler errors;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    errors = mock(LocalizedSecurityErrorHandler.class);
    chain = mock(FilterChain.class);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static MockHttpServletRequest request(String method, String uri, String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(ip);
    return request;
  }

  private void run(jakarta.servlet.Filter filter, MockHttpServletRequest request) throws Exception {
    filter.doFilter(request, new MockHttpServletResponse(), chain);
  }

  @Test
  void loginAttemptsAreLimitedPerIpWithoutAffectingOtherClients() throws Exception {
    PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter(limits, errors);

    for (int i = 0; i < 3; i++) {
      run(filter, request("POST", "/v1/auth/login", "203.0.113.1"));
    }
    run(filter, request("POST", "/v1/auth/login", "203.0.113.2"));

    // 2 passam do primeiro IP, a 3ª é recusada; o segundo IP não é afetado.
    verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, times(1)).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void registrationHasItsOwnHourlyBudgetAndGetsAreNotCounted() throws Exception {
    PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter(limits, errors);

    run(filter, request("POST", "/v1/auth/register", "203.0.113.1"));
    run(filter, request("POST", "/v1/users", "203.0.113.1"));
    // Login ainda tem orçamento próprio, mas o cadastro (1 por hora) já esgotou.
    run(filter, request("POST", "/v1/auth/login", "203.0.113.1"));

    verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, times(1)).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void healthChecksAreNeverLimited() throws Exception {
    PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter(limits, errors);

    for (int i = 0; i < 50; i++) {
      run(filter, request("GET", "/actuator/health", "10.0.0.1"));
    }

    verify(chain, times(50)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, never()).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void heavyRoutesHaveATighterBudgetThanTheGeneralOne() throws Exception {
    AuthenticatedRateLimitFilter filter = new AuthenticatedRateLimitFilter(limits, errors);
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of()));

    for (int i = 0; i < 3; i++) {
      run(filter, request("POST", "/v1/cv/generate", "203.0.113.1"));
    }
    // Rotas comuns do mesmo dono continuam respondendo.
    run(filter, request("GET", "/v1/cv/generations/owner/1", "203.0.113.1"));

    verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, times(1)).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void accountsShareAGeneralBudgetAcrossRoutes() throws Exception {
    AuthenticatedRateLimitFilter filter = new AuthenticatedRateLimitFilter(limits, errors);
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of()));

    for (int i = 0; i < 6; i++) {
      run(filter, request("GET", "/v1/me", "203.0.113.1"));
    }

    verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, times(1)).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void anonymousSessionsOnlyGetTheRouteLimitsNotTheAccountBudget() throws Exception {
    AuthenticatedRateLimitFilter filter = new AuthenticatedRateLimitFilter(limits, errors);
    SecurityContextHolder.getContext().setAuthentication(new AnonymousSessionAuthenticationToken(
        new AnonymousPrincipal(UUID.randomUUID(), UUID.randomUUID(), java.time.Instant.now().plusSeconds(3600))));

    for (int i = 0; i < 20; i++) {
      run(filter, request("GET", "/v1/job-postings/owner/1", "203.0.113.1"));
    }
    run(filter, request("POST", "/v1/job-postings", "203.0.113.1"));
    run(filter, request("POST", "/v1/job-postings", "203.0.113.1"));
    run(filter, request("POST", "/v1/job-postings", "203.0.113.1"));

    verify(chain, times(22)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(errors, times(1)).tooManyRequests(
        org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
        org.mockito.ArgumentMatchers.any(HttpServletResponse.class), anyLong());
  }

  @Test
  void unauthenticatedRequestsPassThroughUntouched() throws Exception {
    AuthenticatedRateLimitFilter filter = new AuthenticatedRateLimitFilter(limits, errors);

    for (int i = 0; i < 10; i++) {
      run(filter, request("POST", "/v1/cv/generate", "203.0.113.1"));
    }

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
