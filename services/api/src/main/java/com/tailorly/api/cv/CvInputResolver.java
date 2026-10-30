package com.tailorly.api.cv;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.base.snapshot.GtSnapshotItem;
import com.tailorly.api.base.snapshot.GtSnapshotItemRepository;
import com.tailorly.api.base.snapshot.GtSnapshotRepository;
import com.tailorly.api.cv.model.FactValue;
import com.tailorly.api.job.persistence.JobPosting;
import com.tailorly.api.job.persistence.JobPostingRepository;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.job.persistence.JobPostingVersionRepository;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolve as entradas de uma geração para o owner: o snapshot aprovado do Ground Truth (com seus
 * fatos congelados) e a versão da vaga. Recursos de outro owner são tratados como inexistentes.
 */
@Component
@RequiredArgsConstructor
class CvInputResolver {
  private final GtSnapshotRepository snapshots;
  private final GtSnapshotItemRepository snapshotItems;
  private final JobPostingRepository postings;
  private final JobPostingVersionRepository jobVersions;

  /** O snapshot pedido ou, sem pedido, o ativo do owner. */
  GtSnapshot resolveSnapshot(Owner owner, UUID requestedId) {
    UUID snapshotId = requestedId != null ? requestedId : owner.getActiveSnapshotId();
    if (snapshotId == null) {
      throw new BadRequestException(ErrorCodes.CvGeneration.NO_ACTIVE_SNAPSHOT);
    }
    GtSnapshot snapshot = snapshots.findById(snapshotId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND));
    // Um snapshot de outro owner é indistinguível de um inexistente.
    if (!snapshot.getOwner().getId().equals(owner.getId())) {
      throw new ResourceNotFoundException(ErrorCodes.GtSnapshot.NOT_FOUND);
    }
    return snapshot;
  }

  /** A versão pedida da vaga ou, sem pedido, a mais recente. */
  JobPostingVersion resolveJobVersion(Owner owner, UUID postingId, Integer requestedVersion) {
    JobPosting posting = postings.findById(postingId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND));
    if (!posting.getOwner().getId().equals(owner.getId())) {
      throw new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND);
    }
    int versionNumber = requestedVersion != null ? requestedVersion : posting.getLatestVersionNumber();
    return jobVersions.findByJobPostingIdAndVersionNumber(postingId, versionNumber)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.CvGeneration.JOB_NOT_FOUND));
  }

  /** Fatos aprovados do snapshot, na ordem estável usada para montar e comparar o currículo. */
  List<FactValue> factsOf(GtSnapshot snapshot) {
    return snapshotItems.findAllBySnapshotIdOrderByCategoryAscFieldKeyAsc(snapshot.getId())
        .stream().map(CvInputResolver::toFact).toList();
  }

  private static FactValue toFact(GtSnapshotItem item) {
    Object value = item.getApprovedValue();
    String text = value instanceof String || value instanceof Number || value instanceof Boolean
        ? value.toString() : null;
    return new FactValue(item.getCategory(), item.getFactRecord().getDisplayOrder(), item.getFieldKey(), text);
  }
}
