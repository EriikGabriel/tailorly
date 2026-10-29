package com.tailorly.api.extraction.interpreting;

import java.util.UUID;

/**
 * Proposta ainda não persistida de um fato encontrado no currículo. A proposta
 * deve ser validada antes de criar {@code Fact}, {@code FactEvidence} e, quando
 * {@code needsReview} for verdadeiro, uma pendência; ela nunca substitui uma
 * correção feita pela pessoa usuária.
 *
 * <p>{@code needsReview} marca fatos realmente ambíguos ou de difícil
 * verificação (ex.: nome dividido em fragmentos, valor inferido de um padrão
 * incerto, conflito entre páginas). Extrações diretas e inequívocas (ex.:
 * e-mail e telefone batidos por regex, um valor copiado verbatim sem
 * reconstrução) não precisam de confirmação manual: a pendência existe para
 * decisões de leitura, não para repetir o que o texto já deixa claro.
 * {@code reviewReason} explica o motivo quando {@code needsReview} é
 * verdadeiro; é {@code null} quando não há pendência.</p>
 */
public record FactProposal(
    String category,
    String fieldKey,
    Object extractedValue,
    UUID extractedPageId,
    String sourceExcerpt,
    int excerptStart,
    int excerptEnd,
    boolean needsReview,
    String reviewReason
) {
  /** Fato inequívoco: não precisa de confirmação manual. */
  public FactProposal(String category, String fieldKey, Object extractedValue, UUID extractedPageId,
      String sourceExcerpt, int excerptStart, int excerptEnd) {
    this(category, fieldKey, extractedValue, extractedPageId, sourceExcerpt, excerptStart, excerptEnd,
        false, null);
  }
}
