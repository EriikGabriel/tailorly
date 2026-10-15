package com.tailorly.api.extraction.dto;

import java.util.UUID;

import com.tailorly.api.extraction.ExtractedPage;
import lombok.Builder;

@Builder
public record ExtractedPageResponse(
    UUID id,
    UUID extractionRunId,
    Integer pageNumber,
    String extractionMethod,
    UUID textObjectId,
    String textHash
) {
  public static ExtractedPageResponse from(ExtractedPage page) {
    return ExtractedPageResponse.builder()
        .id(page.getId())
        .extractionRunId(page.getExtractionRun().getId())
        .pageNumber(page.getPageNumber())
        .extractionMethod(page.getExtractionMethod())
        .textObjectId(page.getTextObject().getId())
        .textHash(page.getTextHash())
        .build();
  }
}
