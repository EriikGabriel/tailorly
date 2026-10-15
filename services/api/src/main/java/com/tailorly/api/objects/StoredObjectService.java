package com.tailorly.api.objects;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.objects.dto.CreateStoredObjectRequest;
import com.tailorly.api.objects.dto.StoredObjectResponse;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoredObjectService {
  private final StoredObjectRepository storedObjectRepository;
  private final OwnerRepository ownerRepository;
  private final OwnerAccessService ownerAccessService;

  @Transactional
  public StoredObjectResponse create(CreateStoredObjectRequest request) {
    Owner owner = getOwner(request.ownerId());
    ownerAccessService.assertCanAccess(owner);

    String deletionStatus = request.deletionStatus() == null || request.deletionStatus().isBlank()
        ? "ACTIVE"
        : request.deletionStatus();

    StoredObject object = StoredObject.builder()
        .owner(owner)
        .purpose(request.purpose())
        .storageKey(request.storageKey())
        .contentHash(request.contentHash())
        .mimeType(request.mimeType())
        .sizeBytes(request.sizeBytes())
        .expiresAt(request.expiresAt())
        .deletionStatus(deletionStatus)
        .build();

    return StoredObjectResponse.from(storedObjectRepository.save(object));
  }

  @Transactional(readOnly = true)
  public StoredObjectResponse findById(UUID id) {
    StoredObject object = getObject(id);
    ownerAccessService.assertCanAccess(object.getOwner());

    return StoredObjectResponse.from(object);
  }

  @Transactional
  public StoredObjectResponse updateDeletionStatus(UUID id, String deletionStatus) {
    StoredObject object = getObject(id);
    ownerAccessService.assertCanAccess(object.getOwner());

    object.setDeletionStatus(deletionStatus);
    return StoredObjectResponse.from(storedObjectRepository.save(object));
  }

  @Transactional(readOnly = true)
  public List<StoredObjectResponse> findAllByOwnerId(UUID ownerId) {
    Owner owner = getOwner(ownerId);
    ownerAccessService.assertCanAccess(owner);

    return storedObjectRepository.findAllByOwnerId(ownerId).stream()
        .map(StoredObjectResponse::from)
        .toList();
  }

  private StoredObject getObject(UUID id) {
    return storedObjectRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.StoredObject.NOT_FOUND));
  }

  private Owner getOwner(UUID id) {
    return ownerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
  }

}
