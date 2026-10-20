/**
 * O arquivo (PDF) do Currículo Base enviado por um owner.
 *
 * <p>{@link com.tailorly.api.base.document.BaseDocumentUploadService} é o fluxo HTTP real de
 * criação: recebe o multipart, valida o PDF, deduplica por SHA-256 dentro do mesmo owner e
 * delega a {@link com.tailorly.api.base.version} a criação/reaproveitamento da versão
 * candidata. {@link com.tailorly.api.base.document.BaseDocumentService} cobre apenas consultas
 * e um cadastro interno por metadados, sem rota HTTP pública.</p>
 */
package com.tailorly.api.base.document;
