package com.tailorly.api.session;

import java.time.Instant;
import java.util.UUID;

/** Identidade de uma sessão anônima já validada pelo servidor. */
public record AnonymousPrincipal(UUID sessionId, UUID ownerId, Instant expiresAt) {
}
