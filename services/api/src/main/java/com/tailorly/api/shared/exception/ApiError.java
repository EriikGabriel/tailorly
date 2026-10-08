package com.tailorly.api.shared.exception;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

@Builder
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<String> details,
    String path
) {
}
