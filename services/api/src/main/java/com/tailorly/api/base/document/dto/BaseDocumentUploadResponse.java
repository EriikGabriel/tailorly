package com.tailorly.api.base.document.dto;

import com.tailorly.api.base.version.dto.BaseVersionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Resposta do upload: combina o {@link BaseDocumentResponse documento} gravado com a
 * {@link BaseVersionResponse versão} candidata criada ou reaproveitada na mesma operação.
 */
@Builder
public record BaseDocumentUploadResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BaseDocumentResponse document,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean reused,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BaseVersionResponse version,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean versionReused
) {
}
