package com.tailorly.api.base.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.BaseDocument;
import lombok.Builder;

@Builder
public record BaseDocumentResponse(
    UUID id, UUID ownerId, UUID originalObjectId, String contentHash,
    String originalFilename, Integer pageCount, Instant createdAt
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
