package com.tailorly.api.shared.security;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, Instant createdAt, Instant expiresAt, String userAgent) {
  static SessionResponse from(UserSession session) {
    return new SessionResponse(
        session.getId(), session.getCreatedAt(), session.getExpiresAt(), session.getUserAgent());
  }
}
