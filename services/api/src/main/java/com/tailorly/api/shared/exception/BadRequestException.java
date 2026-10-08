package com.tailorly.api.shared.exception;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}
