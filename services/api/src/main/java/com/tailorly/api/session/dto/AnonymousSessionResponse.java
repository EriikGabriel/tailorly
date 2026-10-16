package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.session.AnonymousSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AnonymousSessionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant revokedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean valid
) {
  public static AnonymousSessionResponse from(AnonymousSession session, Instant now) {
    boolean valid = session.getRevokedAt() == null && session.getExpiresAt().isAfter(now);
    return AnonymousSessionResponse.builder()
        .id(session.getId())
        .ownerId(session.getOwner().getId())
        .expiresAt(session.getExpiresAt())
        .revokedAt(session.getRevokedAt())
        .valid(valid)
        .build();
  }
}
