package com.tailorly.api.shared.security;

import com.tailorly.api.owner.dto.OwnerResponse;
import com.tailorly.api.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentAccountResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserResponse user,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OwnerResponse owner
) {
}
