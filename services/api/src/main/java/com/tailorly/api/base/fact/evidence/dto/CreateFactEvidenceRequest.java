package com.tailorly.api.base.fact.evidence.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateFactEvidenceRequest(
    @NotNull UUID factId,
    @NotNull UUID extractedPageId,
    @NotBlank String sourceExcerpt,
    @NotNull Object sourceLocator,
    Object boundingBox
) {
}
