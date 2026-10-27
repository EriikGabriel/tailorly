package com.tailorly.api.session.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code replaceActive} precisa ser {@code true} para substituir um Ground Truth ativo que a
 * conta já tenha; sem isso a migração é recusada e nada muda.
 */
public record MigrateAnonymousSessionRequest(
    @NotBlank String sessionToken,
    boolean replaceActive
) {
}
