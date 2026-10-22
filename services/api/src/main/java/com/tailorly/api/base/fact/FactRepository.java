package com.tailorly.api.base.fact;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactRepository extends JpaRepository<Fact, UUID> {
  List<Fact> findAllByFactRecord_Id(UUID recordId);

  List<Fact> findAllByFactRecord_IdOrderByFieldKeyAsc(UUID recordId);

  List<Fact> findAllByExtractionRunId(UUID extractionRunId);
}
