package com.tailorly.api.extraction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtractedPageRepository extends JpaRepository<ExtractedPage, UUID> {
  List<ExtractedPage> findAllByExtractionRunId(UUID extractionRunId);

  List<ExtractedPage> findAllByTextHash(String textHash);

  boolean existsByExtractionRunIdAndPageNumber(UUID extractionRunId, Integer pageNumber);
}
