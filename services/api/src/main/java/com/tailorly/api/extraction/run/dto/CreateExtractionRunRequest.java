package com.tailorly.api.extraction.run.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateExtractionRunRequest(
    @NotNull UUID baseVersionId
) {
}
