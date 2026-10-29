package com.tailorly.api.job.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Informe exatamente um entre {@code url} e {@code text}; {@code title} e {@code company} valem só para texto. */
public record CreateJobPostingRequest(
    @NotNull UUID ownerId,
    @Size(max = 2048) String url,
    @Size(max = 50_000) String text,
    @Size(max = 200) String title,
    @Size(max = 200) String company
) {
}
