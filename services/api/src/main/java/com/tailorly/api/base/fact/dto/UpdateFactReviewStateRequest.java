package com.tailorly.api.base.fact.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateFactReviewStateRequest(@NotBlank String reviewState) {
}
