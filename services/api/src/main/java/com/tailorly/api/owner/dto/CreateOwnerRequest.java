package com.tailorly.api.owner.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CreateOwnerRequest(
    @NotBlank
    String kind,
    UUID userId,
    UUID activeSnapshotId
) {
}
