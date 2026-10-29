package com.tailorly.api.shared.exception;

/** Uma dependência necessária (por exemplo, o compilador de PDF) não está disponível neste ambiente. */
public class ServiceUnavailableException extends RuntimeException {
  public ServiceUnavailableException(String message) {
    super(message);
  }
}
