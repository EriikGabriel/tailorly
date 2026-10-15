package com.tailorly.api.objects.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.objects.StoredObject;
import lombok.Builder;

@Builder
public record StoredObjectResponse(
    UUID id,
    UUID ownerId,
    String purpose,
    String storageKey,
    String contentHash,
    String mimeType,
    Long sizeBytes,
    Instant expiresAt,
    String deletionStatus
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
