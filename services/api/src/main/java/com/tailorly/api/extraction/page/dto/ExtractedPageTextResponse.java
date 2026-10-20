package com.tailorly.api.extraction.page.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record ExtractedPageTextResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pageId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String text
) {}
