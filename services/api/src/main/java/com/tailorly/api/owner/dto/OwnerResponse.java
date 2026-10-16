package com.tailorly.api.owner.dto;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record OwnerResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String kind,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) UUID userId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) UUID activeSnapshotId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int revision,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant expiresAt
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
