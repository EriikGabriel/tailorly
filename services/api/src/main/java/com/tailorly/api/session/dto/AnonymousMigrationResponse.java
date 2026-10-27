package com.tailorly.api.session.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AnonymousMigrationResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID snapshotId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID baseVersionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID documentId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean documentReused,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean replacedActive
) {
}
