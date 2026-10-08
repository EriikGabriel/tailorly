package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AnonymousSessionValidationResponse(
    boolean valid,
    UUID ownerId,
    Instant expiresAt
) {
  public static AnonymousSessionValidationResponse invalid() {
    return AnonymousSessionValidationResponse.builder().valid(false).build();
  }
}
