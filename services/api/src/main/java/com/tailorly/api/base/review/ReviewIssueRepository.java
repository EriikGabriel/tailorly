package com.tailorly.api.base.review;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewIssueRepository extends JpaRepository<ReviewIssue, UUID> {
  List<ReviewIssue> findAllByBaseVersionIdOrderById(UUID baseVersionId);

  List<ReviewIssue> findAllByBaseVersionIdAndStatusOrderById(UUID baseVersionId, String status);

  List<ReviewIssue> findAllByBaseVersionIdAndBlockingTrueAndStatusIn(
      UUID baseVersionId, List<String> statuses);

  boolean existsByBaseVersionIdAndBlockingTrueAndStatus(UUID baseVersionId, String status);
}
