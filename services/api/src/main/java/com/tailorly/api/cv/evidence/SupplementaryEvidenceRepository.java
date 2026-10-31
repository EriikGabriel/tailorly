package com.tailorly.api.cv.evidence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplementaryEvidenceRepository extends JpaRepository<SupplementaryEvidence, UUID> {
  List<SupplementaryEvidence> findAllBySnapshotIdOrderByCollectedAtAsc(UUID snapshotId);
}
