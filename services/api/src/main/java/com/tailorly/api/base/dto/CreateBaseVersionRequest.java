package com.tailorly.api.base.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBaseVersionRequest(
    @NotNull UUID ownerId,
    @NotNull UUID documentId,
    @Positive int versionNumber,
    @NotBlank String status
) {
}
