package com.tailorly.api.diligence.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code companyName} é obrigatório; {@code jobPostingId} é opcional e vincula a análise a uma vaga do owner. */
public record CreateDueDiligenceRequest(
    @NotNull UUID ownerId,
    @Size(min = 2, max = 200) String companyName,
    UUID jobPostingId
) {
}
