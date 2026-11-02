package com.tailorly.api.shared.ratelimit;

import com.tailorly.api.shared.security.LocalizedSecurityErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans dos limites de taxa. Os filtros rodam só dentro da cadeia de segurança (registro como
 * filtro de servlet desligado) e ficam em uma {@code @Configuration} para que testes de fatia web
 * que importam apenas a segurança não precisem carregá-los.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

  @Bean
  @ConditionalOnProperty(name = "tailorly.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
  public PublicEndpointRateLimitFilter publicEndpointRateLimitFilter(
      RateLimitProperties properties, LocalizedSecurityErrorHandler errorHandler) {
    return new PublicEndpointRateLimitFilter(properties, errorHandler);
  }

  @Bean
  @ConditionalOnProperty(name = "tailorly.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
  public AuthenticatedRateLimitFilter authenticatedRateLimitFilter(
      RateLimitProperties properties, LocalizedSecurityErrorHandler errorHandler) {
    return new AuthenticatedRateLimitFilter(properties, errorHandler);
  }

  @Bean
  @ConditionalOnProperty(name = "tailorly.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
  public FilterRegistrationBean<PublicEndpointRateLimitFilter> publicRateLimitRegistration(
      PublicEndpointRateLimitFilter filter) {
    FilterRegistrationBean<PublicEndpointRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  @ConditionalOnProperty(name = "tailorly.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
  public FilterRegistrationBean<AuthenticatedRateLimitFilter> authenticatedRateLimitRegistration(
      AuthenticatedRateLimitFilter filter) {
    FilterRegistrationBean<AuthenticatedRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
