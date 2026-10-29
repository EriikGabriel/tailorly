package com.tailorly.api.job.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingVersionRepository extends JpaRepository<JobPostingVersion, UUID> {
  Optional<JobPostingVersion> findByJobPostingIdAndVersionNumber(UUID jobPostingId, int versionNumber);

  List<JobPostingVersion> findAllByJobPostingIdOrderByVersionNumberDesc(UUID jobPostingId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from JobPostingVersion v where v.jobPosting.id = :jobPostingId")
  int deleteAllByJobPostingId(@Param("jobPostingId") UUID jobPostingId);
}
