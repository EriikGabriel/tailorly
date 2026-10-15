package com.tailorly.api.objects.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateStoredObjectRequest(
    @NotNull UUID ownerId,
    @NotBlank String purpose,
    @NotBlank String storageKey,
    @NotBlank String contentHash,
    @NotBlank String mimeType,
    @NotNull @PositiveOrZero Long sizeBytes,
    Instant expiresAt,
    String deletionStatus
) {
}
