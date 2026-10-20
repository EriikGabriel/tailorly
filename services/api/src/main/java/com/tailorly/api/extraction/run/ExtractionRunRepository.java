package com.tailorly.api.extraction.run;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {
  Optional<ExtractionRun> findFirstByBaseVersionIdAndPipelineFingerprintAndStatusIn(
      UUID versionId, String fingerprint, List<String> statuses);

  List<ExtractionRun> findAllByBaseVersionIdOrderByCreatedAtDesc(UUID versionId);

  @Query(value = """
      select * from extraction_runs
      where status = 'QUEUED' and pipeline_versions->>'implementation' = 'selective-pdf-v1'
      order by created_at, id limit 1 for update skip locked
      """, nativeQuery = true)
  Optional<ExtractionRun> findNextQueued();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ExtractionRun r where r.id = :id")
  Optional<ExtractionRun> findForUpdate(@Param("id") UUID id);

  @Modifying
  @Query(value = """
      update extraction_runs set status = 'FAILED', error_code = 'WORKER_TIMEOUT', finished_at = :now
      where status = 'PROCESSING' and started_at < :cutoff
        and pipeline_versions->>'implementation' = 'selective-pdf-v1'
      """, nativeQuery = true)
  int expireStalled(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
