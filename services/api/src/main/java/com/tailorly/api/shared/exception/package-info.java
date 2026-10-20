/**
 * Exceções de domínio ({@link com.tailorly.api.shared.exception.BadRequestException},
 * {@link com.tailorly.api.shared.exception.ResourceNotFoundException},
 * {@link com.tailorly.api.shared.exception.ResourceAlreadyExistsException}), os códigos de
 * mensagem localizável em {@link com.tailorly.api.shared.exception.ErrorCodes} e o formato de
 * resposta de erro {@link com.tailorly.api.shared.exception.ApiError}.
 *
 * <p>Essas exceções são lançadas pelos serviços de cada domínio e traduzidas em respostas HTTP
 * por {@link com.tailorly.api.shared.handler.GlobalExceptionHandler}.</p>
 */
package com.tailorly.api.shared.exception;
