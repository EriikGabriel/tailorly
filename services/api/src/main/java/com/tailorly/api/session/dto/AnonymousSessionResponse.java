package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.session.AnonymousSession;
import lombok.Builder;

@Builder
public record AnonymousSessionResponse(
    UUID id,
    UUID ownerId,
    Instant expiresAt,
    Instant revokedAt,
    boolean valid
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
