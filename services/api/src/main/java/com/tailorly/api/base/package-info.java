/**
 * Currículo Base: o arquivo enviado pelo usuário e suas versões, fonte dos fatos pessoais e
 * profissionais que alimentam a geração de currículos (o template Typst controla apenas a
 * apresentação — veja "Decisão de produto" no {@code README.md} do monorepo).
 *
 * <p>O domínio é dividido em dois subpacotes para refletir dois agregados distintos:</p>
 * <ul>
 *   <li>{@link com.tailorly.api.base.document} — o arquivo original e sua deduplicação por
 *       hash dentro do mesmo owner.</li>
 *   <li>{@link com.tailorly.api.base.version} — cada versão (candidata, extraída ou ativa) de
 *       um documento, com o número e o status controlados pelo servidor.</li>
 * </ul>
 *
 * <p>A extração de texto/OCR de uma versão vive em {@link com.tailorly.api.extraction}; a
 * revisão de fatos e a ativação do Ground Truth ainda são fluxo planejado.</p>
 */
package com.tailorly.api.base;
