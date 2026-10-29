package com.tailorly.api.shared.exception;

import java.util.List;

/** A requisição é válida, mas o conteúdo que ela referencia não pôde ser obtido, entendido ou compilado. */
public class UnprocessableRequestException extends RuntimeException {
  private final List<String> details;

  public UnprocessableRequestException(String message) {
    this(message, List.of());
  }

  /** {@code details} são diagnósticos para quem enviou o conteúdo (por exemplo, erros do compilador). */
  public UnprocessableRequestException(String message, List<String> details) {
    super(message);
    this.details = List.copyOf(details);
  }

  public List<String> getDetails() {
    return details;
  }
}
