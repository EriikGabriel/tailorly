package com.tailorly.api.base.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtSnapshotRepository extends JpaRepository<GtSnapshot, UUID> {
  List<GtSnapshot> findAllByOwnerIdOrderBySnapshotNumberDesc(UUID ownerId);

  Optional<GtSnapshot> findFirstByOwnerIdOrderBySnapshotNumberDesc(UUID ownerId);

  Optional<GtSnapshot> findByIdAndOwnerId(UUID id, UUID ownerId);
}
