package com.tailorly.api.cv.evidence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Decisão da pessoa sobre uma evidência complementar. Só {@code confirmado} pode entrar no currículo. */
public record UpdateEvidenceReviewStateRequest(
    @NotNull @Pattern(regexp = "pendente|confirmado|descartado") String reviewState
) {
}
