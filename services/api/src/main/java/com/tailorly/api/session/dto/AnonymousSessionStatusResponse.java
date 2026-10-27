package com.tailorly.api.session.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Estado da sessão anônima atual: validade, uso consumido, limites vigentes e se existe um
 * snapshot revisado que pode ser migrado para uma conta antes da expiração.
 */
@Builder
public record AnonymousSessionStatusResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID sessionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean migratable,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Usage usage,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Limits limits
) {
  public record Usage(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int uploads,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int extractions,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int generations
  ) {
  }

  public record Limits(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long maxFileBytes,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int maxPages,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int maxUploads,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int maxExtractions,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int maxGenerations
  ) {
  }
}
