package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta da emissão de uma sessão anônima. O {@code token} só é devolvido aqui, uma vez; o
 * servidor guarda apenas o hash. Enviar como {@code Authorization: Bearer <token>}.
 */
public record IssuedAnonymousSessionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID sessionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String token,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt
) {
}
