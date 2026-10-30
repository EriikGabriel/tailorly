package com.tailorly.api.base.snapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorly.api.base.fact.Fact;
import com.tailorly.api.base.fact.FactRepository;
import com.tailorly.api.base.review.ReviewDecision;
import com.tailorly.api.base.review.ReviewDecisionRepository;
import com.tailorly.api.base.review.ReviewIssueRepository;
import com.tailorly.api.base.snapshot.dto.GtSnapshotItemResponse;
import com.tailorly.api.base.snapshot.dto.GtSnapshotResponse;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.util.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/**
 * Congela os fatos revisados e troca o snapshot ativo do owner em uma única transação.
 *
 * <p>O lock do owner serializa duas aprovações concorrentes. O snapshot anterior continua
 * disponível para auditoria, mas deixa de ser a versão ativa quando a nova aprovação termina.</p>
 */
public class GtSnapshotService {
  private static final String EXTRACTED = "EXTRACTED";
  private static final String ACTIVE = "ACTIVE";
  private static final String SUPERSEDED = "SUPERSEDED";
  private static final int SCHEMA_VERSION = 1;

  private final GtSnapshotRepository snapshots;
  private final GtSnapshotItemRepository items;
  private final BaseVersionRepository versions;
  private final OwnerRepository owners;
  private final FactRepository facts;
  private final ReviewDecisionRepository decisions;
  private final ReviewIssueRepository issues;
  private final OwnerAccessService access;
  private final ObjectMapper objectMapper;

  @Transactional
  public GtSnapshotResponse approve(UUID baseVersionId) {
    BaseVersion initialVersion = versions.findById(baseVersionId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
    UUID ownerId = initialVersion.getOwner().getId();
    Owner owner = owners.findByIdForUpdate(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    access.assertCanAccess(owner);
    BaseVersion version = versions.findByIdForUpdate(baseVersionId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));

    if (!version.getOwner().getId().equals(ownerId) || !EXTRACTED.equals(version.getStatus())) {
      throw new BadRequestException(ErrorCodes.BaseVersion.INVALID_STATUS);
    }
    if (issues.existsByBaseVersionIdAndBlockingTrueAndStatus(baseVersionId, "PENDING")) {
      throw new BadRequestException(ErrorCodes.BaseVersion.REVIEW_PENDING);
    }

    List<Fact> versionFacts = facts.findAllByBaseVersionId(baseVersionId);
    if (versionFacts.isEmpty()) {
      throw new BadRequestException(ErrorCodes.BaseVersion.NO_APPROVED_FACTS);
    }
    Map<UUID, ReviewDecision> latestDecisions = latestDecisions(baseVersionId);
    List<Fact> approvedFacts = new ArrayList<>();
    for (Fact fact : versionFacts) {
      if ("pendente".equals(fact.getReviewState())) {
        throw new BadRequestException(ErrorCodes.BaseVersion.REVIEW_PENDING);
      }
      if (!List.of("confirmado", "corrigido").contains(fact.getReviewState())) continue;
      ReviewDecision decision = latestDecisions.get(fact.getId());
      if (decision == null
          || !fact.getReviewState().equals(decision.getDecisionResult())
          || !java.util.Objects.equals(fact.getReviewedValue(), decision.getApprovedValue())) {
        throw new BadRequestException(ErrorCodes.BaseVersion.DECISION_MISSING);
      }
      approvedFacts.add(fact);
    }
    if (approvedFacts.isEmpty()) {
      throw new BadRequestException(ErrorCodes.BaseVersion.NO_APPROVED_FACTS);
    }

    int snapshotNumber = snapshots.findFirstByOwnerIdOrderBySnapshotNumberDesc(ownerId)
        .map(snapshot -> Math.addExact(snapshot.getSnapshotNumber(), 1))
        .orElse(1);
    String contentHash = contentHash(approvedFacts);
    Instant approvedAt = Instant.now();
    GtSnapshot snapshot = snapshots.saveAndFlush(GtSnapshot.builder()
        .owner(owner)
        .baseVersion(version)
        .snapshotNumber(snapshotNumber)
        .approvedReviewRevision(version.getReviewRevision())
        .contentHash(contentHash)
        .approvedAt(approvedAt)
        .schemaVersion(SCHEMA_VERSION)
        .build());

    List<GtSnapshotItem> snapshotItems = approvedFacts.stream()
        .map(fact -> GtSnapshotItem.builder()
            .snapshot(snapshot)
            .sourceFact(fact)
            .sourceDecision(latestDecisions.get(fact.getId()))
            .factRecord(fact.getFactRecord())
            .category(fact.getFactRecord().getCategory())
            .fieldKey(fact.getFieldKey())
            .approvedValue(fact.getReviewedValue())
            .build())
        .toList();
    List<GtSnapshotItem> savedItems = items.saveAll(snapshotItems);

    UUID previousSnapshotId = owner.getActiveSnapshotId();
    if (previousSnapshotId != null) {
      snapshots.findByIdAndOwnerIdForUpdate(previousSnapshotId, ownerId).ifPresent(previous -> {
        if (!previous.getBaseVersion().getId().equals(baseVersionId)) {
          BaseVersion previousVersion = versions.findByIdForUpdate(previous.getBaseVersion().getId())
              .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
          previousVersion.setStatus(SUPERSEDED);
          versions.save(previousVersion);
        }
      });
    }
    version.setStatus(ACTIVE);
    version.setTemporalReviewRequired(false);
    versions.save(version);
    if (owners.updateActiveSnapshot(ownerId, snapshot.getId()) == 0) {
      throw new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND);
    }

    List<GtSnapshotItemResponse> itemResponses = savedItems.stream()
        .map(GtSnapshotItemResponse::from).toList();
    return GtSnapshotResponse.from(snapshot, itemResponses, true, ownerId, baseVersionId);
  }

  @Transactional(readOnly = true)
  public GtSnapshotResponse findById(UUID id) {
    GtSnapshot snapshot = snapshots.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND));
    access.assertCanAccess(snapshot.getOwner());
    List<GtSnapshotItemResponse> itemResponses = items
        .findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(id).stream()
        .map(GtSnapshotItemResponse::from).toList();
    UUID ownerId = snapshot.getOwner().getId();
    UUID versionId = snapshot.getBaseVersion().getId();
    return GtSnapshotResponse.from(snapshot, itemResponses,
        id.equals(snapshot.getOwner().getActiveSnapshotId()), ownerId, versionId);
  }

  private Map<UUID, ReviewDecision> latestDecisions(UUID versionId) {
    Map<UUID, ReviewDecision> latest = new HashMap<>();
    decisions.findAllByBaseVersionIdOrderByReviewRevisionAsc(versionId).stream()
        .filter(decision -> decision.getFact() != null)
        .forEach(decision -> latest.put(decision.getFact().getId(), decision));
    return latest;
  }

  private String contentHash(List<Fact> approvedFacts) {
    try {
      List<HashEntry> entries = approvedFacts.stream()
          .sorted(Comparator.comparing((Fact fact) -> fact.getFactRecord().getCategory())
              .thenComparing(fact -> fact.getFactRecord().getDisplayOrder())
              .thenComparing(Fact::getFieldKey))
          .map(fact -> new HashEntry(
              fact.getFactRecord().getCategory(),
              fact.getFactRecord().getDisplayOrder(),
              fact.getFieldKey(),
              fact.getReviewedValue()))
          .toList();
      byte[] bytes = objectMapper.writeValueAsBytes(entries);
      return Sha256.hex(bytes);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Não foi possível calcular o hash do snapshot aprovado do Ground Truth", exception);
    }
  }

  private record HashEntry(String category, int displayOrder, String fieldKey, Object value) {
  }
}
