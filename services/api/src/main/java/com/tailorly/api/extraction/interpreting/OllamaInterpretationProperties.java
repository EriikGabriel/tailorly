package com.tailorly.api.extraction.interpreting;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tailorly.interpretation.ollama")
public record OllamaInterpretationProperties(String endpoint, String model, String apiKey) {}
