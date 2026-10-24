package com.tailorly.api.base.snapshot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshot;

public record GtSnapshotResponse(
    UUID id,
    UUID ownerId,
    UUID baseVersionId,
    int snapshotNumber,
    int approvedReviewRevision,
    String contentHash,
    Instant approvedAt,
    int schemaVersion,
    boolean active,
    List<GtSnapshotItemResponse> items
) {
  public static GtSnapshotResponse from(
      GtSnapshot snapshot,
      List<GtSnapshotItemResponse> items,
      boolean active,
      UUID ownerId,
      UUID baseVersionId
  ) {
    return new GtSnapshotResponse(
        snapshot.getId(), ownerId, baseVersionId, snapshot.getSnapshotNumber(),
        snapshot.getApprovedReviewRevision(), snapshot.getContentHash(), snapshot.getApprovedAt(),
        snapshot.getSchemaVersion(), active, items);
  }
}
