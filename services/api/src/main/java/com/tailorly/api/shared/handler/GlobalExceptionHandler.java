package com.tailorly.api.shared.handler;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.tailorly.api.shared.exception.ApiError;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private String localize(String codeOrMessage) {
    return messageSource.getMessage(
        codeOrMessage,
        null,
        codeOrMessage,
        LocaleContextHolder.getLocale()
    );
  }

  private ResponseEntity<ApiError> buildError(
      HttpStatus status,
      String message,
      HttpServletRequest request
  ) {
    return buildError(status, message, null, request);
  }

  private ResponseEntity<ApiError> buildError(
      HttpStatus status,
      String message,
      List<String> details,
      HttpServletRequest request
  ) {
    ApiError error = ApiError.builder()
        .timestamp(Instant.now())
        .status(status.value())
        .error(localize(ErrorCodes.HTTP_PREFIX + status.name().toLowerCase(Locale.ROOT)))
        .message(message)
        .details(details)
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(status).body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request
  ) {
    return buildError(HttpStatus.NOT_FOUND, localize(ex.getMessage()), request);
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleConflict(
      ResourceAlreadyExistsException ex,
      HttpServletRequest request
  ) {
    return buildError(HttpStatus.CONFLICT, localize(ex.getMessage()), request);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(
      BadRequestException ex,
      HttpServletRequest request
  ) {
    return buildError(HttpStatus.BAD_REQUEST, localize(ex.getMessage()), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleForbidden(
      AccessDeniedException ex,
      HttpServletRequest request
  ) {
    return buildError(HttpStatus.FORBIDDEN, localize(ex.getMessage()), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(
      AuthenticationException ex,
      HttpServletRequest request
  ) {
    return buildError(
        HttpStatus.UNAUTHORIZED,
        localize(ErrorCodes.Security.AUTHENTICATION_REQUIRED),
        request
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    List<String> details = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + localize(error.getDefaultMessage()))
        .distinct()
        .toList();

    return buildError(
        HttpStatus.BAD_REQUEST,
        localize(ErrorCodes.Validation.FAILED),
        details,
        request
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request
  ) {
    return buildError(
        HttpStatus.BAD_REQUEST,
        localize(ErrorCodes.Validation.REQUEST),
        request
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableMessage(
      HttpMessageNotReadableException ex,
      HttpServletRequest request
  ) {
    return buildError(
        HttpStatus.BAD_REQUEST,
        localize(ErrorCodes.Request.MALFORMED),
        request
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception ex,
      HttpServletRequest request
  ) {
    // Registre ex no logger para investigação.
    return buildError(
        HttpStatus.INTERNAL_SERVER_ERROR,
        localize(ErrorCodes.UNEXPECTED),
        request
    );
  }
}
