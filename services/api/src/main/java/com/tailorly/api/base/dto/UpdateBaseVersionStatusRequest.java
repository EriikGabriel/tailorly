package com.tailorly.api.base.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBaseVersionStatusRequest(@NotBlank String status) {
}
