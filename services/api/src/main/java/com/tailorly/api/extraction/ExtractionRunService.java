package com.tailorly.api.extraction;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.BaseVersion;
import com.tailorly.api.base.BaseVersionRepository;
import com.tailorly.api.extraction.dto.CreateExtractionRunRequest;
import com.tailorly.api.extraction.dto.ExtractionRunResponse;
import com.tailorly.api.extraction.dto.FailExtractionRunRequest;
import com.tailorly.api.extraction.dto.UpdateExtractionRunStatusRequest;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtractionRunService {
  private final ExtractionRunRepository extractionRunRepository;
  private final BaseVersionRepository baseVersionRepository;
  private final StoredObjectRepository storedObjectRepository;
  private final OwnerAccessService ownerAccessService;

  @Transactional
  public ExtractionRunResponse create(CreateExtractionRunRequest request) {
    BaseVersion version = getVersion(request.baseVersionId());
    ownerAccessService.assertCanAccess(version.getOwner());

    StoredObject outputObject = null;

    if (request.outputObjectId() != null) {
      outputObject = storedObjectRepository.findById(request.outputObjectId())
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.StoredObject.NOT_FOUND));

      if (!version.getOwner().getId().equals(outputObject.getOwner().getId())) {
        throw new AccessDeniedException(ErrorCodes.ExtractionRun.OUTPUT_OBJECT_OWNER_MISMATCH);
      }
    }

    ExtractionRun run = ExtractionRun.builder()
        .baseVersion(version)
        .pipelineFingerprint(request.pipelineFingerprint())
        .pipelineVersions(request.pipelineVersions())
        .outputObject(outputObject)
        .status(request.status())
        .startedAt(Instant.now())
        .build();

    return ExtractionRunResponse.from(extractionRunRepository.save(run));
  }

  @Transactional(readOnly = true)
  public ExtractionRunResponse findById(UUID id) {
    ExtractionRun run = getRun(id);

    ownerAccessService.assertCanAccess(run.getBaseVersion().getOwner());
    return ExtractionRunResponse.from(run);
  }

  @Transactional
  public ExtractionRunResponse updateStatus(UUID id, UpdateExtractionRunStatusRequest request) {
    ExtractionRun run = getRun(id);

    ownerAccessService.assertCanAccess(run.getBaseVersion().getOwner());
    run.setStatus(request.status());
    return ExtractionRunResponse.from(extractionRunRepository.save(run));
  }

  @Transactional
  public ExtractionRunResponse fail(UUID id, FailExtractionRunRequest request) {
    ExtractionRun run = getRun(id);
    ownerAccessService.assertCanAccess(run.getBaseVersion().getOwner());

    run.setStatus("FAILED");
    run.setErrorCode(request.errorCode());
    run.setFinishedAt(Instant.now());
    
    return ExtractionRunResponse.from(extractionRunRepository.save(run));
  }

  private ExtractionRun getRun(UUID id) {
    return extractionRunRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractionRun.NOT_FOUND));
  }

  private BaseVersion getVersion(UUID id) {
    return baseVersionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }
}
