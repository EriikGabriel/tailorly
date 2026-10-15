package com.tailorly.api.extraction;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {
}
