package com.tailorly.api.base.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.BaseDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record BaseDocumentResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID ownerId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID originalObjectId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String contentHash,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String originalFilename,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"integer", "null"}) Integer pageCount,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
  public static BaseDocumentResponse from(BaseDocument document) {
    return BaseDocumentResponse.builder()
        .id(document.getId())
        .ownerId(document.getOwner().getId())
        .originalObjectId(document.getOriginalObject().getId())
        .contentHash(document.getContentHash())
        .originalFilename(document.getOriginalFilename())
        .pageCount(document.getPageCount())
        .createdAt(document.getCreatedAt())
        .build();
  }
}
