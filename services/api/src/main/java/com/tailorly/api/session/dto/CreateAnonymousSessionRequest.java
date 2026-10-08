package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnonymousSessionRequest(
    @NotNull
    UUID ownerId,
    @NotBlank
    String tokenHash,
    @NotNull
    Instant expiresAt
) {
}
