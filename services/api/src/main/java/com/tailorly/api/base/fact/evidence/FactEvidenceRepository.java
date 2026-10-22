package com.tailorly.api.base.fact.evidence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactEvidenceRepository extends JpaRepository<FactEvidence, UUID> {
  List<FactEvidence> findAllByFactId(UUID factId);

  List<FactEvidence> findAllByExtractedPageId(UUID extractedPageId);
}
