package com.tailorly.api.base.fact.record.dto;

import java.util.UUID;

import com.tailorly.api.base.fact.record.FactRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FactRecordResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID baseVersionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String category,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int displayOrder
) {
  public static FactRecordResponse from(FactRecord record) {
    return FactRecordResponse.builder()
        .id(record.getId())
        .baseVersionId(record.getBaseVersion().getId())
        .category(record.getCategory())
        .displayOrder(record.getDisplayOrder())
        .build();
  }
}
