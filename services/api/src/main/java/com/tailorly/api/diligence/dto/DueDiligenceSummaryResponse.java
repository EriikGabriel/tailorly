package com.tailorly.api.diligence.dto;

import java.time.Instant;
import java.util.UUID;

/** Item de listagem: contagem de fatores com evidências, sem o relatório completo. */
public record DueDiligenceSummaryResponse(
    UUID id,
    String status,
    String triggerKind,
    String companyName,
    String jobTitle,
    UUID jobPostingId,
    int factorsWithEvidence,
    int factorsTotal,
    Instant createdAt
) {
}
