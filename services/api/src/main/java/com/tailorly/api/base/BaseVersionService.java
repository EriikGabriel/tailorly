package com.tailorly.api.base;

import java.util.UUID;

import com.tailorly.api.base.dto.BaseVersionResponse;
import com.tailorly.api.base.dto.CreateBaseVersionRequest;
import com.tailorly.api.base.dto.UpdateBaseVersionStatusRequest;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseVersionService {
  private final BaseVersionRepository baseVersionRepository;
  private final BaseDocumentRepository baseDocumentRepository;
  private final OwnerRepository ownerRepository;
  private final OwnerAccessService ownerAccessService;

  @Transactional
  public BaseVersionResponse create(CreateBaseVersionRequest request) {
    Owner owner = ownerRepository.findById(request.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    ownerAccessService.assertCanAccess(owner);

    if (baseVersionRepository.existsByOwnerIdAndVersionNumber(owner.getId(), request.versionNumber())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.BaseVersion.VERSION_ALREADY_EXISTS);
    }

    BaseDocument document = baseDocumentRepository.findById(request.documentId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseDocument.NOT_FOUND));

    if (!owner.getId().equals(document.getOwner().getId())) {
      throw new AccessDeniedException(ErrorCodes.BaseVersion.DOCUMENT_OWNER_MISMATCH);
    }

    BaseVersion version = BaseVersion.builder()
        .owner(owner)
        .document(document)
        .versionNumber(request.versionNumber())
        .status(request.status())
        .reviewRevision(0)
        .build();

    return BaseVersionResponse.from(baseVersionRepository.save(version));
  }


  @Transactional(readOnly = true)
  public BaseVersionResponse findById(UUID id) {
    BaseVersion version = getVersion(id);
    ownerAccessService.assertCanAccess(version.getOwner());

    return BaseVersionResponse.from(version);
  }

  @Transactional
  public BaseVersionResponse updateStatus(UUID id, UpdateBaseVersionStatusRequest request) {
    BaseVersion version = getVersion(id);

    ownerAccessService.assertCanAccess(version.getOwner());
    version.setStatus(request.status());

    return BaseVersionResponse.from(baseVersionRepository.save(version));
  }

  @Transactional
  public BaseVersionResponse incrementReviewRevision(UUID id) {
    BaseVersion version = getVersion(id);

    ownerAccessService.assertCanAccess(version.getOwner());
    version.setReviewRevision(version.getReviewRevision() + 1);

    return BaseVersionResponse.from(baseVersionRepository.save(version));
  }

  private BaseVersion getVersion(UUID id) {
    return baseVersionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }
}
