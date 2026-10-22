package com.tailorly.api.base.fact.group.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateDisplayOrderRequest(@NotNull @Min(0) Integer displayOrder) {
}
