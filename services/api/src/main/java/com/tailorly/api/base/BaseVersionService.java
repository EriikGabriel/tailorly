package com.tailorly.api.base;

import java.util.UUID;

import com.tailorly.api.base.dto.BaseVersionResponse;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseVersionService {
  static final String CANDIDATE = "CANDIDATE";

  private final BaseVersionRepository baseVersionRepository;
  private final OwnerAccessService ownerAccessService;

  VersionSelection findOrCreateForUpload(BaseDocument document) {
    var existing = baseVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId());
    if (existing.isPresent()) {
      BaseVersion version = existing.get();

      version.setTemporalReviewRequired(true);
      return new VersionSelection(baseVersionRepository.saveAndFlush(version), true);
    }

    int nextNumber = baseVersionRepository
        .findFirstByOwnerIdOrderByVersionNumberDesc(document.getOwner().getId())
        .map(version -> Math.addExact(version.getVersionNumber(), 1))
        .orElse(1);
    BaseVersion candidate = BaseVersion.builder()
        .owner(document.getOwner())
        .document(document)
        .versionNumber(nextNumber)
        .status(CANDIDATE)
        .reviewRevision(0)
        .temporalReviewRequired(true)
        .build();
    return new VersionSelection(baseVersionRepository.saveAndFlush(candidate), false);
  }

  @Transactional(readOnly = true)
  public BaseVersionResponse findById(UUID id) {
    BaseVersion version = getVersion(id);
    ownerAccessService.assertCanAccess(version.getOwner());

    return BaseVersionResponse.from(version);
  }

  private BaseVersion getVersion(UUID id) {
    return baseVersionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }

  record VersionSelection(BaseVersion version, boolean reused) {
  }
}
