/**
 * Versões do Currículo Base: candidata, extraída ou ativa.
 *
 * <p>Número e status de uma versão são sempre definidos pelo servidor; não há rota pública para
 * alterá-los livremente. {@link com.tailorly.api.base.version.BaseVersionService} cuida apenas
 * da seleção/criação da versão no momento do upload — a transição para {@code EXTRACTED} é
 * responsabilidade de {@code com.tailorly.api.extraction.ExtractionQueue}, e a ativação como
 * Ground Truth (que exigiria revisão de fatos) ainda é fluxo planejado.</p>
 */
package com.tailorly.api.base.version;
