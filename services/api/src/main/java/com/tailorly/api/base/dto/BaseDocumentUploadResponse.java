package com.tailorly.api.base.dto;

import lombok.Builder;

@Builder
public record BaseDocumentUploadResponse(BaseDocumentResponse document, boolean reused) {
}
