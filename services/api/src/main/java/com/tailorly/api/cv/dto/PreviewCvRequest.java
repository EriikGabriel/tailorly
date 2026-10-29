package com.tailorly.api.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código Typst a compilar com o conteúdo de uma geração existente, sem salvar nada. */
public record PreviewCvRequest(@NotBlank @Size(max = 64_000) String content) {
}
