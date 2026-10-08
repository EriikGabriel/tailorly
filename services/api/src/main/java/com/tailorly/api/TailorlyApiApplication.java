package com.tailorly.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Tailorly API",
        version = "0.1.0",
        description = "API HTTP do Tailorly. Endpoints de domínio ainda estão planejados."))
public class TailorlyApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(TailorlyApiApplication.class, args);
  }
}
