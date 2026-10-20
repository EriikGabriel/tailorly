package com.tailorly.api.extraction.run;

import java.util.List;
import java.util.UUID;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.extraction.ExtractionQueue;
import com.tailorly.api.extraction.run.dto.CreateExtractionRunRequest;
import com.tailorly.api.extraction.run.dto.ExtractionRunResponse;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtractionRunService {
  private final ExtractionRunRepository runs;
  private final BaseVersionRepository versions;
  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final ExtractionQueue queue;

  @Transactional
  public ExtractionRunResponse create(CreateExtractionRunRequest request) {
    BaseVersion version = getVersion(request.baseVersionId());
    access.assertCanAccess(version.getOwner());
    owners.findByIdForUpdate(version.getOwner().getId()).orElseThrow();
    return ExtractionRunResponse.from(queue.enqueue(version));
  }

  @Transactional(readOnly = true)
  public ExtractionRunResponse findById(UUID id) {
    ExtractionRun run = runs.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractionRun.NOT_FOUND));
    access.assertCanAccess(run.getBaseVersion().getOwner());
    return ExtractionRunResponse.from(run);
  }

  @Transactional(readOnly = true)
  public List<ExtractionRunResponse> findByVersion(UUID baseVersionId) {
    access.assertCanAccess(getVersion(baseVersionId).getOwner());
    return runs.findAllByBaseVersionIdOrderByCreatedAtDesc(baseVersionId).stream()
        .map(ExtractionRunResponse::from).toList();
  }

  private BaseVersion getVersion(UUID id) {
    return versions.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }
}
