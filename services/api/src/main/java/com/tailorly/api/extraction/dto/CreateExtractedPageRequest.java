package com.tailorly.api.extraction.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateExtractedPageRequest(
    @NotNull UUID extractionRunId,
    @Positive Integer pageNumber,
    @NotBlank String extractionMethod,
    @NotNull UUID textObjectId,
    @NotBlank String textHash
) {
}
