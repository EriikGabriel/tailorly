package com.tailorly.api.extraction.interpreting;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tailorly.interpretation.openai")
public record OpenAiInterpretationProperties(String endpoint, String model, String apiKey) {}
