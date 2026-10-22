package com.tailorly.api.base.fact.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateFactRequest(
    @NotNull UUID recordId,
    UUID extractionRunId,
    @NotBlank String fieldKey,
    Object extractedValue,
    Object reviewedValue,
    @NotBlank String origin,
    @NotBlank String reviewState
) {
}
