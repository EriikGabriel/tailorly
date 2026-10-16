package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AnonymousSessionValidationResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean valid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant expiresAt
) {
  public static AnonymousSessionValidationResponse invalid() {
    return AnonymousSessionValidationResponse.builder().valid(false).build();
  }
}
