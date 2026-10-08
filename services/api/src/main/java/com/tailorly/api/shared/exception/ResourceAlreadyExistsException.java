package com.tailorly.api.shared.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
  public ResourceAlreadyExistsException(String message) {
    super(message);
  }
}
