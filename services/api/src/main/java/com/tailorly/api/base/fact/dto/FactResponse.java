package com.tailorly.api.base.fact.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.fact.Fact;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FactResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID recordId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) UUID extractionRunId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fieldKey,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"object", "array", "string", "number", "boolean", "null"}) Object extractedValue,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"object", "array", "string", "number", "boolean", "null"}) Object reviewedValue,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String origin,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reviewState,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant confirmedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant revalidateAfter
) {
  public static FactResponse from(Fact fact) {
    return FactResponse.builder()
        .id(fact.getId())
        .recordId(fact.getFactRecord().getId())
        .extractionRunId(fact.getExtractionRun() == null ? null : fact.getExtractionRun().getId())
        .fieldKey(fact.getFieldKey())
        .extractedValue(fact.getExtractedValue())
        .reviewedValue(fact.getReviewedValue())
        .origin(fact.getOrigin())
        .reviewState(fact.getReviewState())
        .confirmedAt(fact.getConfirmedAt())
        .revalidateAfter(fact.getRevalidateAfter())
        .build();
  }
}
