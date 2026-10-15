package com.tailorly.api.objects.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDeletionStatusRequest(@NotBlank String deletionStatus) {
}
