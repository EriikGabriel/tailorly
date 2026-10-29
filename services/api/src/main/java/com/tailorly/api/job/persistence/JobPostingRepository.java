package com.tailorly.api.job.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {
  Optional<JobPosting> findByOwnerIdAndSourceKey(UUID ownerId, String sourceKey);

  List<JobPosting> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

  long countByOwnerId(UUID ownerId);
}
