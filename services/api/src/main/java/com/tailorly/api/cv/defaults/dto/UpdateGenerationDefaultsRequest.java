package com.tailorly.api.cv.defaults.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Layout padrão da conta: nome de um template embarcado (ex.: {@code classic}, {@code harvard}). */
public record UpdateGenerationDefaultsRequest(
    @NotBlank @Size(max = 64) String templateName
) {
}
