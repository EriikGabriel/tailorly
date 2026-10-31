package com.tailorly.api.cv.defaults.dto;

/** Padrões da conta; {@code templateName} nulo quando a conta ainda não escolheu um layout. */
public record GenerationDefaultsResponse(String templateName) {
}
