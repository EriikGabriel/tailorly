package com.tailorly.api.base.snapshot.dto;

import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshotItem;

public record GtSnapshotItemResponse(
    UUID id,
    UUID sourceFactId,
    UUID sourceDecisionId,
    UUID recordId,
    String category,
    String fieldKey,
    Object approvedValue
) {
  public static GtSnapshotItemResponse from(GtSnapshotItem item) {
    return new GtSnapshotItemResponse(
        item.getId(),
        item.getSourceFact().getId(),
        item.getSourceDecision().getId(),
        item.getFactRecord().getId(),
        item.getCategory(),
        item.getFieldKey(),
        item.getApprovedValue());
  }
}
