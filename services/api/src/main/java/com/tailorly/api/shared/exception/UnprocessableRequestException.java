package com.tailorly.api.shared.exception;

/** A requisição é válida, mas o conteúdo externo que ela referencia não pôde ser obtido ou entendido. */
public class UnprocessableRequestException extends RuntimeException {
  public UnprocessableRequestException(String message) {
    super(message);
  }
}
