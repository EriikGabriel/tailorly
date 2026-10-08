package com.tailorly.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
    @NotBlank
    @Email
    String email
) {
}