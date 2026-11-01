package com.tailorly.api.cv.evidence.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.cv.evidence.SupplementaryEvidence;

public record SupplementaryEvidenceResponse(
    UUID id,
    String sourceKind,
    String sourceUrl,
    String subjectKey,
    String subjectLabel,
    String sourceExcerpt,
    String claim,
    String reviewState,
    Instant collectedAt
) {
  public static SupplementaryEvidenceResponse from(SupplementaryEvidence evidence) {
    return new SupplementaryEvidenceResponse(evidence.getId(), evidence.getSourceKind(), evidence.getSourceUrl(),
        evidence.getSubjectKey(), evidence.getSubjectLabel(),
        evidence.getSourceExcerpt(), evidence.getClaim(), evidence.getReviewState(), evidence.getCollectedAt());
  }
}
