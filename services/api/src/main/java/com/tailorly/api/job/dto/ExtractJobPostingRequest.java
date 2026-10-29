package com.tailorly.api.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractJobPostingRequest(
    @NotBlank @Size(max = 2048) String url
) {
}
