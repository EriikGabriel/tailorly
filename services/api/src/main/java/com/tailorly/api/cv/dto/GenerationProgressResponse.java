package com.tailorly.api.cv.dto;

import java.time.Instant;

import com.tailorly.api.cv.progress.GenerationProgress;
import io.swagger.v3.oas.annotations.media.Schema;

/** Etapa da geração em curso: {@code status} RUNNING, DONE ou FAILED; {@code stage} é o rótulo da etapa. */
public record GenerationProgressResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String stage,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"}) String detail,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int round,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startedAt
) {
  public static GenerationProgressResponse from(GenerationProgress.Snapshot snapshot) {
    return new GenerationProgressResponse(snapshot.status(), snapshot.stage(), snapshot.detail(), snapshot.round(),
        snapshot.startedAt());
  }
}
