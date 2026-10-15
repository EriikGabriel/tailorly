package com.tailorly.api.base.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBaseDocumentRequest(
    @NotNull UUID ownerId,
    @NotNull UUID originalObjectId,
    @NotBlank String contentHash,
    @NotBlank String originalFilename,
    @Positive Integer pageCount
) {
}
