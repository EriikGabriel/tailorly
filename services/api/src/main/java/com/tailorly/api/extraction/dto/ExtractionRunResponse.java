package com.tailorly.api.extraction.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tailorly.api.extraction.ExtractionRun;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ExtractionRunResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID baseVersionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String pipelineFingerprint,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Object> pipelineVersions,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) UUID outputObjectId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) Instant finishedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String errorCode
) {
  public static ExtractionRunResponse from(ExtractionRun run) {
    return ExtractionRunResponse.builder()
        .id(run.getId())
        .baseVersionId(run.getBaseVersion().getId())
        .pipelineFingerprint(run.getPipelineFingerprint())
        .pipelineVersions(run.getPipelineVersions())
        .outputObjectId(run.getOutputObject() == null ? null : run.getOutputObject().getId())
        .status(run.getStatus())
        .startedAt(run.getStartedAt())
        .finishedAt(run.getFinishedAt())
        .errorCode(run.getErrorCode())
        .build();
  }
}
