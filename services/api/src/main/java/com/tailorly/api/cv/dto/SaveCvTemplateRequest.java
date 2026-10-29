package com.tailorly.api.cv.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveCvTemplateRequest(
    @NotNull UUID ownerId,
    @NotBlank @Size(max = 64_000) String content
) {
}
