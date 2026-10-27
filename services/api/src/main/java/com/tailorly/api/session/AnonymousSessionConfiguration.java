package com.tailorly.api.session;

import java.time.Duration;

import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Beans do acesso anônimo. Fica em uma {@code @Configuration} (e não em {@code @Component})
 * para que testes de fatia web que importam só a segurança não precisem carregá-los.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AnonymousProperties.class)
public class AnonymousSessionConfiguration {

  /** Taxa de requisições por sessão anônima. */
  @Bean
  public FixedWindowRateLimiter anonymousRequestLimiter(AnonymousProperties properties) {
    return new FixedWindowRateLimiter(properties.requestsPerMinute(), Duration.ofMinutes(1));
  }

  /** Emissão de sessões por IP; protege o storage de criação em massa de sessões. */
  @Bean
  public FixedWindowRateLimiter anonymousSessionIssueLimiter(AnonymousProperties properties) {
    return new FixedWindowRateLimiter(properties.sessionsPerIpPerHour(), Duration.ofHours(1));
  }

  @Bean
  public AnonymousSessionAuthenticationFilter anonymousSessionAuthenticationFilter(
      AnonymousSessionService sessionService,
      FixedWindowRateLimiter anonymousRequestLimiter,
      LocalizedSecurityErrorHandler errorHandler) {
    return new AnonymousSessionAuthenticationFilter(sessionService, anonymousRequestLimiter, errorHandler);
  }

  /** O filtro roda só dentro da cadeia de segurança; evita o registro automático como filtro de servlet. */
  @Bean
  public FilterRegistrationBean<AnonymousSessionAuthenticationFilter> anonymousFilterRegistration(
      AnonymousSessionAuthenticationFilter filter) {
    FilterRegistrationBean<AnonymousSessionAuthenticationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  @ConditionalOnProperty(name = "tailorly.anonymous.cleanup-enabled", havingValue = "true",
      matchIfMissing = true)
  public AnonymousCleanupJob anonymousCleanupJob(AnonymousSessionRepository sessions,
      AnonymousDataPurgeService purge) {
    return new AnonymousCleanupJob(sessions, purge);
  }
}
