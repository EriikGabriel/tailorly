package com.tailorly.api.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record BaseDocumentUploadResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BaseDocumentResponse document,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean reused
) {
}
