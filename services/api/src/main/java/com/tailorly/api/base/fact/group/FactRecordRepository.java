package com.tailorly.api.base.fact.group;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactRecordRepository extends JpaRepository<FactRecord, UUID> {
  List<FactRecord> findAllByBaseVersionIdOrderByDisplayOrderAsc(UUID baseVersionId);
}
