package com.tailorly.api.base.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.BaseVersion;
import lombok.Builder;

@Builder
public record BaseVersionResponse(
    UUID id,
    UUID ownerId,
    UUID documentId,
    int versionNumber,
    String status,
    int reviewRevision,
    Instant createdAt
) {
  public static BaseVersionResponse from(BaseVersion version) {
    return BaseVersionResponse.builder()
        .id(version.getId())
        .ownerId(version.getOwner().getId())
        .documentId(version.getDocument().getId())
        .versionNumber(version.getVersionNumber())
        .status(version.getStatus())
        .reviewRevision(version.getReviewRevision())
        .createdAt(version.getCreatedAt())
        .build();
  }
}
