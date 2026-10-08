package com.tailorly.api.shared.config;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  @Bean
  public LocaleResolver localeResolver() {
    return new AcceptHeaderLocaleResolver() {
      @Override
      public Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
          return Locale.ENGLISH;
        }

        Locale requestedLocale = request.getLocale();
        return "pt".equalsIgnoreCase(requestedLocale.getLanguage())
            ? Locale.forLanguageTag("pt-BR")
            : Locale.ENGLISH;
      }
    };
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/v1/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Authorization", "Content-Type", "Accept-Language")
        .allowCredentials(true);
  }
}
