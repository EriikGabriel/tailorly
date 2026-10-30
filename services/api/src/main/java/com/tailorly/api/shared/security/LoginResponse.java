package com.tailorly.api.shared.security;

import java.time.Instant;

import com.tailorly.api.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record LoginResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tokenType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserResponse user,
    /** Token opaco para {@code POST /v1/auth/refresh}; devolvido em texto puro só aqui. */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String refreshToken
) {
}
