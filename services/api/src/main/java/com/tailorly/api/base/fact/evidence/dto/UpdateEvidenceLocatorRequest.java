package com.tailorly.api.base.fact.evidence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateEvidenceLocatorRequest(@NotNull Object sourceLocator) {
}
