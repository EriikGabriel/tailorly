package com.tailorly.api.user.dto;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UserResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> roles
) {
  public static UserResponse from(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .enabled(user.isEnabled())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .sorted()
            .toList())
        .build();
  }
}
