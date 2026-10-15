package com.tailorly.api.extraction.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExtractionRunRequest(
    @NotNull UUID baseVersionId,
    @NotBlank String pipelineFingerprint,
    @NotNull Map<String, Object> pipelineVersions,
    UUID outputObjectId,
    @NotBlank String status
) {
}
