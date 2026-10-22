package com.tailorly.api.base.fact.evidence.dto;

import java.util.UUID;

import com.tailorly.api.base.fact.evidence.FactEvidence;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FactEvidenceResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID factId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID extractedPageId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String sourceExcerpt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Object sourceLocator,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"object", "array", "string", "number", "boolean", "null"}) Object boundingBox
) {
  public static FactEvidenceResponse from(FactEvidence evidence) {
    return FactEvidenceResponse.builder()
        .id(evidence.getId())
        .factId(evidence.getFact().getId())
        .extractedPageId(evidence.getExtractedPage().getId())
        .sourceExcerpt(evidence.getSourceExcerpt())
        .sourceLocator(evidence.getSourceLocator())
        .boundingBox(evidence.getBoundingBox())
        .build();
  }
}
