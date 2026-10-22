package com.tailorly.api.base.review;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, UUID> {
  List<ReviewDecision> findAllByBaseVersionIdOrderByReviewRevisionAsc(UUID baseVersionId);

  List<ReviewDecision> findAllByFactIdOrderByReviewRevisionAsc(UUID factId);
}
