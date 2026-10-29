package com.tailorly.api.cv;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvGenerationRepository extends JpaRepository<CvGeneration, UUID> {
  @EntityGraph(attributePaths = {"snapshot", "snapshot.baseVersion", "jobPostingVersion", "jobPostingVersion.jobPosting", "template", "pdfObject", "markdownObject", "owner"})
  Optional<CvGeneration> findByOwnerIdAndInputHash(UUID ownerId, String inputHash);

  @EntityGraph(attributePaths = {"snapshot", "snapshot.baseVersion", "jobPostingVersion", "jobPostingVersion.jobPosting", "template", "pdfObject", "markdownObject", "owner"})
  List<CvGeneration> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  @EntityGraph(attributePaths = {"snapshot", "snapshot.baseVersion", "jobPostingVersion", "jobPostingVersion.jobPosting", "template", "pdfObject", "markdownObject", "owner"})
  Optional<CvGeneration> findWithDetailsById(UUID id);
}
