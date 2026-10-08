package com.tailorly.api.owner.dto;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import lombok.Builder;

@Builder
public record OwnerResponse(
    UUID id,
    String kind,
    UUID userId,
    UUID activeSnapshotId,
    int revision,
    Instant expiresAt
) {
  public static OwnerResponse from(Owner owner) {
    return OwnerResponse.builder()
        .id(owner.getId())
        .kind(owner.getKind().name().substring("KIND_".length()).toLowerCase(Locale.ROOT))
        .userId(owner.getUser() == null ? null : owner.getUser().getId())
        .activeSnapshotId(owner.getActiveSnapshotId())
        .revision(owner.getRevision())
        .expiresAt(owner.getExpiresAt())
        .build();
  }
}
