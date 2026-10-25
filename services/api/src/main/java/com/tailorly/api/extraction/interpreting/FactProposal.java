package com.tailorly.api.extraction.interpreting;

import java.util.UUID;

/**
 * Proposta ainda não persistida de um fato encontrado no currículo. A proposta
 * deve ser validada antes de criar {@code Fact}, {@code FactEvidence} e uma
 * pendência; ela nunca substitui uma correção feita pela pessoa usuária.
 */
public record FactProposal(
    String category,
    String fieldKey,
    Object extractedValue,
    UUID extractedPageId,
    String sourceExcerpt,
    int excerptStart,
    int excerptEnd
) {}
