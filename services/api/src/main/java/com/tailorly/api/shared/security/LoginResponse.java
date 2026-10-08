package com.tailorly.api.shared.security;

import java.time.Instant;

import com.tailorly.api.user.dto.UserResponse;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
}
