package com.tailorly.api.base.snapshot;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtSnapshotItemRepository extends JpaRepository<GtSnapshotItem, UUID> {
  List<GtSnapshotItem> findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(UUID snapshotId);
}
