package com.tailorly.api.shared.exception;

/** Limite de uso ou de taxa atingido; respondida como HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
  public TooManyRequestsException(String message) {
    super(message);
  }
}
