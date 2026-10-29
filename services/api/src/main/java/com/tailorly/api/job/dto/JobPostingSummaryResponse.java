package com.tailorly.api.job.dto;

import java.time.Instant;
import java.util.UUID;

public record JobPostingSummaryResponse(
    UUID id,
    UUID ownerId,
    String sourceKind,
    String title,
    String companyName,
    int latestVersionNumber,
    Boolean expired,
    Instant createdAt,
    Instant updatedAt
) {
}
