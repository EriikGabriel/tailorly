package com.tailorly.api.base.fact.group.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateFactRecordRequest(
    @NotNull UUID baseVersionId,
    @NotBlank String category,
    @NotNull @Min(0) Integer displayOrder
) {
}
