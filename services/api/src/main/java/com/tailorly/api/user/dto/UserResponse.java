package com.tailorly.api.user.dto;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.user.User;
import lombok.Builder;

@Builder
public record UserResponse(
    UUID id,
    String email,
    boolean enabled,
    List<String> roles
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
