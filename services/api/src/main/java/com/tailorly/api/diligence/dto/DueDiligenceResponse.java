package com.tailorly.api.diligence.dto;

import java.time.Instant;
import java.util.UUID;

public record DueDiligenceResponse(
    UUID id,
    UUID ownerId,
    String companyName,
    UUID jobPostingId,
    Integer jobVersionNumber,
    String analyzerVersion,
    String status,
    String triggerKind,
    DueDiligenceReport report,
    Instant createdAt
) {
}
