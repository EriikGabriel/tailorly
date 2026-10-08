package com.tailorly.api.session.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateAnonymousSessionRequest(@NotBlank String tokenHash) {
}
