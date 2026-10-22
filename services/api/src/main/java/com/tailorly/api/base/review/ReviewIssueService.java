package com.tailorly.api.base.review;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.fact.FactRepository;
import com.tailorly.api.base.fact.dto.UpdateFactReviewStateRequest;
import com.tailorly.api.base.review.dto.ReviewIssueResponse;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewIssueService {
  private static final Set<String> RESULTS = Set.of("confirmado", "corrigido", "omitido");

  private final ReviewIssueRepository issues;
  private final ReviewDecisionRepository decisions;
  private final BaseVersionRepository versions;
  private final FactRepository facts;
  private final OwnerAccessService access;

  @Transactional(readOnly = true)
  public List<ReviewIssueResponse> findByVersion(UUID versionId) {
    BaseVersion version = versions.findById(versionId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
    access.assertCanAccess(version.getOwner());
    return issues.findAllByBaseVersionIdOrderById(versionId).stream()
        .map(ReviewIssueResponse::from).toList();
  }

  @Transactional
  public ReviewIssueResponse answer(UUID id, UpdateFactReviewStateRequest request) {
    ReviewIssue issue = issues.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Fact.NOT_FOUND));
    if (issue.getFact() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }
    BaseVersion version = versions.findByIdForUpdate(issue.getBaseVersion().getId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
    access.assertCanAccess(version.getOwner());
    if (!"CANDIDATE".equals(version.getStatus()) && !"EXTRACTED".equals(version.getStatus())) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }
    if (!issue.getFact().getFactRecord().getBaseVersion().getId().equals(version.getId())) {
      throw new BadRequestException(ErrorCodes.Fact.RECORD_VERSION_MISMATCH);
    }
    if (!"PENDING".equals(issue.getStatus())) return ReviewIssueResponse.from(issue);

    String result = request.reviewState() == null
        ? "" : request.reviewState().trim().toLowerCase(Locale.ROOT);
    if (!RESULTS.contains(result)) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }
    var fact = issue.getFact();
    Object previousValue = fact.getReviewedValue() != null
        ? fact.getReviewedValue() : fact.getExtractedValue();
    Object approvedValue = null;
    if ("corrigido".equals(result)) {
      if (request.reviewedValue() == null) {
        throw new BadRequestException(ErrorCodes.Fact.INVALID_CORRECTION);
      }
      fact.setReviewedValue(request.reviewedValue());
      approvedValue = request.reviewedValue();
      fact.setConfirmedAt(null);
    } else if ("confirmado".equals(result)) {
      if (fact.getExtractedValue() == null && fact.getReviewedValue() == null) {
        throw new BadRequestException(ErrorCodes.Fact.INVALID_CONFIRMATION);
      }
      if (fact.getReviewedValue() == null) fact.setReviewedValue(fact.getExtractedValue());
      fact.setConfirmedAt(Instant.now());
      approvedValue = fact.getReviewedValue();
    } else {
      fact.setConfirmedAt(null);
    }

    fact.setReviewState(result);
    issue.setStatus("RESOLVED");
    facts.save(fact);
    int revision = Math.addExact(version.getReviewRevision(), 1);
    version.setReviewRevision(revision);
    decisions.save(ReviewDecision.builder()
        .baseVersion(version)
        .fact(fact)
        .issue(issue)
        .actorOwner(version.getOwner())
        .decisionResult(result)
        .previousValue(previousValue)
        .approvedValue(approvedValue)
        .reviewRevision(revision)
        .decidedAt(Instant.now())
        .build());

    return ReviewIssueResponse.from(issue);
  }
}
