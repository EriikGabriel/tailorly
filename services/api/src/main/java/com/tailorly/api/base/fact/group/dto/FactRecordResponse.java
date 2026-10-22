package com.tailorly.api.base.fact.group.dto;

import java.util.UUID;

import com.tailorly.api.base.fact.group.FactRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FactRecordResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID baseVersionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String category,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int displayOrder
) {
  public static FactRecordResponse from(FactRecord factRecord) {
    return FactRecordResponse.builder()
        .id(factRecord.getId())
        .baseVersionId(factRecord.getBaseVersion().getId())
        .category(factRecord.getCategory())
        .displayOrder(factRecord.getDisplayOrder())
        .build();
  }
}
