package com.tailorly.api.base.fact;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.fact.dto.CreateFactRequest;
import com.tailorly.api.base.fact.dto.FactResponse;
import com.tailorly.api.base.fact.dto.UpdateFactReviewStateRequest;
import com.tailorly.api.base.fact.group.FactRecord;
import com.tailorly.api.base.fact.group.FactRecordRepository;
import com.tailorly.api.base.review.ReviewDecision;
import com.tailorly.api.base.review.ReviewDecisionRepository;
import com.tailorly.api.base.review.ReviewIssue;
import com.tailorly.api.base.review.ReviewIssueRepository;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FactService {
  private static final String PENDING = "pendente";
  private static final String CONFIRMED = "confirmado";
  private static final String CORRECTED = "corrigido";
  private static final String OMITTED = "omitido";
  private static final Set<String> REVIEW_STATES = Set.of(PENDING, CONFIRMED, CORRECTED, OMITTED);

  private final FactRepository facts;
  private final FactRecordRepository records;
  private final ExtractionRunRepository runs;
  private final BaseVersionRepository versions;
  private final ReviewDecisionRepository decisions;
  private final ReviewIssueRepository issues;
  private final OwnerAccessService access;

  @Transactional
  public FactResponse create(CreateFactRequest request) {
    FactRecord factRecord = findFactRecord(request.recordId());
    BaseVersion version = versions.findByIdForUpdate(factRecord.getBaseVersion().getId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
    access.assertCanAccess(version.getOwner());
    assertReviewable(version);

    ExtractionRun run = request.extractionRunId() == null ? null : getRun(request.extractionRunId());
    if (run != null && !run.getBaseVersion().getId().equals(version.getId())) {
      throw new BadRequestException(ErrorCodes.Fact.EXTRACTION_VERSION_MISMATCH);
    }
    String state = normalizeReviewState(request.reviewState());
    if (CONFIRMED.equals(state)
        && request.reviewedValue() == null && request.extractedValue() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_CONFIRMATION);
    }
    if (CORRECTED.equals(state) && request.reviewedValue() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_CORRECTION);
    }
    Instant confirmedAt = CONFIRMED.equals(state) ? Instant.now() : null;
    Object reviewedValue = request.reviewedValue();
    if (CONFIRMED.equals(state) && reviewedValue == null) {
      reviewedValue = request.extractedValue();
    }

    Fact fact = Fact.builder()
        .factRecord(factRecord)
        .extractionRun(run)
        .fieldKey(request.fieldKey().trim())
        .extractedValue(request.extractedValue())
        .reviewedValue(reviewedValue)
        .origin(request.origin().trim())
        .reviewState(state)
        .confirmedAt(confirmedAt)
        .build();

    facts.save(fact);

    if (PENDING.equals(state)) {
      issues.save(ReviewIssue.builder()
          .baseVersion(version)
          .fact(fact)
          .category(factRecord.getCategory())
          .reason("Confirme, corrija ou omita este valor antes de usá-lo.")
          .blocking(true)
          .status("PENDING")
          .questionAndChoices(java.util.Map.of(
              "question", "Revise este valor antes de usá-lo.",
              "choices", java.util.List.of("confirmado", "corrigido", "omitido")))
          .build());
    }

    if (!PENDING.equals(state)) {
      int revision = Math.addExact(version.getReviewRevision(), 1);
      version.setReviewRevision(revision);
      versions.save(version);
      decisions.save(ReviewDecision.builder()
          .baseVersion(version)
          .fact(fact)
          .actorOwner(version.getOwner())
          .decisionResult(state)
          .approvedValue(CONFIRMED.equals(state) || CORRECTED.equals(state)
              ? fact.getReviewedValue() : null)
          .reviewRevision(revision)
          .decidedAt(Instant.now())
          .build());
    }

    return FactResponse.from(fact);
  }

  @Transactional(readOnly = true)
  public FactResponse findById(UUID id) {
    Fact fact = getFact(id);
    access.assertCanAccess(fact.getFactRecord().getBaseVersion().getOwner());
    return FactResponse.from(fact);
  }

  @Transactional(readOnly = true)
  public java.util.List<FactResponse> findByRecord(UUID recordId) {
    FactRecord record = findFactRecord(recordId);
    access.assertCanAccess(record.getBaseVersion().getOwner());
    return facts.findAllByFactRecord_IdOrderByFieldKeyAsc(recordId).stream()
        .map(FactResponse::from).toList();
  }

  @Transactional
  public FactResponse updateReviewState(UUID id, UpdateFactReviewStateRequest request) {
    Fact fact = getFact(id);
    BaseVersion version = lockVersion(fact);

    access.assertCanAccess(version.getOwner());
    assertReviewable(version);
    applyDecision(fact, version, normalizeReviewState(request.reviewState()),
        request.reviewedValue());

    return FactResponse.from(facts.save(fact));
  }

  @Transactional
  public FactResponse confirm(UUID id) {
    Fact fact = getFact(id);
    BaseVersion version = lockVersion(fact);

    access.assertCanAccess(version.getOwner());
    assertReviewable(version);
    if (fact.getExtractedValue() == null && fact.getReviewedValue() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_CONFIRMATION);
    }

    applyDecision(fact, version, CONFIRMED, null);
    if (fact.getReviewedValue() == null) {
      fact.setReviewedValue(fact.getExtractedValue());
    }

    return FactResponse.from(facts.save(fact));
  }

  private void applyDecision(Fact fact, BaseVersion version, String newState, Object newValue) {
    String previousState = fact.getReviewState();
    Object currentValue = fact.getReviewedValue();
    if (newState.equals(previousState)
        && (newValue == null || java.util.Objects.equals(newValue, currentValue))) {
      return;
    }

    Object previousValue = currentValue != null ? currentValue : fact.getExtractedValue();
    if (newValue != null && CORRECTED.equals(newState)) {
      fact.setReviewedValue(newValue);
    }

    if (CONFIRMED.equals(newState)
        && fact.getReviewedValue() == null && fact.getExtractedValue() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_CONFIRMATION);
    }

    if (CORRECTED.equals(newState) && fact.getReviewedValue() == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_CORRECTION);
    }

    if (CONFIRMED.equals(newState)) {
      if (fact.getReviewedValue() == null) {
        fact.setReviewedValue(fact.getExtractedValue());
      }
      fact.setConfirmedAt(Instant.now());
    } else {
      fact.setConfirmedAt(null);
    }

    fact.setReviewState(newState);

    issues.findAllByBaseVersionIdAndStatusOrderById(version.getId(), "PENDING").stream()
        .filter(issue -> issue.getFact() != null && issue.getFact().getId().equals(fact.getId()))
        .forEach(issue -> issue.setStatus("RESOLVED"));

    int revision = Math.addExact(version.getReviewRevision(), 1);
    version.setReviewRevision(revision);
    versions.save(version);
    decisions.save(ReviewDecision.builder()
        .baseVersion(version)
        .fact(fact)
        .actorOwner(version.getOwner())
        .decisionResult(newState)
        .previousValue(previousValue)
        .approvedValue(CONFIRMED.equals(newState) || CORRECTED.equals(newState)
            ? fact.getReviewedValue() : null)
        .reviewRevision(revision)
        .decidedAt(Instant.now())
        .build());
  }

  private String normalizeReviewState(String state) {
    if (state == null) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }

    String normalized = state.trim().toLowerCase(Locale.ROOT);
    if (!REVIEW_STATES.contains(normalized)) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }
    
    return normalized;
  }

  private Fact getFact(UUID id) {
    return facts.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Fact.NOT_FOUND));
  }

  private FactRecord findFactRecord(UUID id) {
    return records.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.FactRecord.NOT_FOUND));
  }

  private ExtractionRun getRun(UUID id) {
    return runs.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractionRun.NOT_FOUND));
  }

  private BaseVersion lockVersion(Fact fact) {
    UUID versionId = fact.getFactRecord().getBaseVersion().getId();
    return versions.findByIdForUpdate(versionId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }

  private void assertReviewable(BaseVersion version) {
    if (!"CANDIDATE".equals(version.getStatus()) && !"EXTRACTED".equals(version.getStatus())) {
      throw new BadRequestException(ErrorCodes.Fact.INVALID_REVIEW_STATE);
    }
  }
}
