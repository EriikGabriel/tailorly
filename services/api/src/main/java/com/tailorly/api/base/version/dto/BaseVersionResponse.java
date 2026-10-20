package com.tailorly.api.base.version.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.version.BaseVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record BaseVersionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID documentId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int versionNumber,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int reviewRevision,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean temporalReviewRequired,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
  public static BaseVersionResponse from(BaseVersion version) {
    return BaseVersionResponse.builder()
        .id(version.getId())
        .ownerId(version.getOwner().getId())
        .documentId(version.getDocument().getId())
        .versionNumber(version.getVersionNumber())
        .status(version.getStatus())
        .reviewRevision(version.getReviewRevision())
        .temporalReviewRequired(version.isTemporalReviewRequired())
        .createdAt(version.getCreatedAt())
        .build();
  }
}
