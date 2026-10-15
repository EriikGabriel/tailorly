package com.tailorly.api.extraction.dto;

import jakarta.validation.constraints.NotBlank;

public record FailExtractionRunRequest(@NotBlank String errorCode) {
}
