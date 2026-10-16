package com.tailorly.api.objects.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.objects.StoredObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record StoredObjectResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String purpose,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String storageKey,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String contentHash,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String mimeType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sizeBytes,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String deletionStatus
) {
  public static StoredObjectResponse from(StoredObject object) {
    return StoredObjectResponse.builder()
        .id(object.getId())
        .ownerId(object.getOwner().getId())
        .purpose(object.getPurpose())
        .storageKey(object.getStorageKey())
        .contentHash(object.getContentHash())
        .mimeType(object.getMimeType())
        .sizeBytes(object.getSizeBytes())
        .expiresAt(object.getExpiresAt())
        .deletionStatus(object.getDeletionStatus())
        .build();
  }
}
