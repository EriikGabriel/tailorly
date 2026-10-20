package com.tailorly.api.extraction.page.dto;

import java.util.UUID;

import com.tailorly.api.extraction.page.ExtractedPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ExtractedPageResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID extractionRunId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"integer", "null"}) Integer pageNumber,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String extractionMethod,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID textObjectId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String textHash
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
