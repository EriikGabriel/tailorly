package com.tailorly.api.extraction.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tailorly.api.extraction.ExtractionRun;
import lombok.Builder;

@Builder
public record ExtractionRunResponse(
    UUID id,
    UUID baseVersionId,
    String pipelineFingerprint,
    Map<String, Object> pipelineVersions,
    UUID outputObjectId,
    String status,
    Instant startedAt,
    Instant finishedAt,
    String errorCode
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
