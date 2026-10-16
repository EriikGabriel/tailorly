package com.tailorly.api.shared.config;

import java.beans.Introspector;
import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
  @Bean
  public OperationCustomizer stableOperationIds() {
    return (operation, handlerMethod) -> {
      String controller = handlerMethod.getBeanType().getSimpleName()
          .replaceFirst("Controller$", "");
      String method = handlerMethod.getMethod().getName();
      operation.setOperationId(Introspector.decapitalize(controller) + capitalize(method));
      operation.setTags(List.of(controller));
      return operation;
    };
  }

  private String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
