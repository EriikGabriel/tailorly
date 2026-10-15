package com.tailorly.api.extraction.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateExtractionRunStatusRequest(@NotBlank String status) {
}
