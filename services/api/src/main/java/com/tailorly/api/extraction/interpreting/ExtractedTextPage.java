package com.tailorly.api.extraction.interpreting;

import java.util.UUID;

/**
 * Entrada imutável para um interpretador semântico. O texto continua associado
 * à página e ao método de extração para que toda proposta tenha evidência.
 */
public record ExtractedTextPage(
    UUID extractedPageId,
    int pageNumber,
    String extractionMethod,
    String text
) {}
