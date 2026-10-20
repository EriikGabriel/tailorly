package com.tailorly.api.base.document;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.document.dto.BaseDocumentResponse;
import com.tailorly.api.base.document.dto.CreateBaseDocumentRequest;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
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

/**
 * Operações de metadados do {@link BaseDocument} que não envolvem gravar bytes no storage.
 *
 * <p>{@link #create} é usado apenas internamente (sem rota HTTP pública); o fluxo HTTP real de
 * criação é o upload em {@link BaseDocumentUploadService}, que garante que o arquivo exista no
 * storage antes de registrar o documento.</p>
 */
@Service
@RequiredArgsConstructor
public class BaseDocumentService {
  private final BaseDocumentRepository baseDocumentRepository;
  private final OwnerRepository ownerRepository;
  private final StoredObjectRepository storedObjectRepository;
  private final OwnerAccessService ownerAccessService;

  @Transactional
  public BaseDocumentResponse create(CreateBaseDocumentRequest request) {
    Owner owner = ownerRepository.findById(request.ownerId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    ownerAccessService.assertCanAccess(owner);

    if (baseDocumentRepository.existsByOwnerIdAndContentHash(owner.getId(), request.contentHash())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.BaseDocument.CONTENT_ALREADY_EXISTS);
    }

    StoredObject originalObject = storedObjectRepository.findById(request.originalObjectId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.StoredObject.NOT_FOUND));

    if (!owner.getId().equals(originalObject.getOwner().getId())) {
      throw new AccessDeniedException(ErrorCodes.Owner.ACCESS_DENIED);
    }

    BaseDocument document = BaseDocument.builder()
        .owner(owner)
        .originalObject(originalObject)
        .contentHash(request.contentHash())
        .originalFilename(request.originalFilename())
        .pageCount(request.pageCount())
        .build();

    return BaseDocumentResponse.from(baseDocumentRepository.save(document));
  }

  @Transactional(readOnly = true)
  public BaseDocumentResponse findById(UUID id) {
    BaseDocument document = getBaseDocument(id);

    ownerAccessService.assertCanAccess(document.getOwner());
    return BaseDocumentResponse.from(document);
  }

  private BaseDocument getBaseDocument(UUID id) {
    return baseDocumentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseDocument.NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<BaseDocumentResponse> findAllByOwnerId(UUID ownerId) {
    Owner owner = ownerRepository.findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));

    ownerAccessService.assertCanAccess(owner);

    return baseDocumentRepository.findAllByOwnerId(ownerId).stream()
        .map(BaseDocumentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public BaseDocumentResponse findByContentHash(String contentHash) {
    ownerAccessService.assertAdmin();

    List<BaseDocument> documents = baseDocumentRepository.findAllByContentHash(contentHash);
    if (documents.isEmpty()) {
      throw new ResourceNotFoundException(ErrorCodes.BaseDocument.NOT_FOUND);
    }

    if (documents.size() > 1) {
      throw new ResourceAlreadyExistsException(ErrorCodes.BaseDocument.HASH_NOT_UNIQUE);
    }

    return BaseDocumentResponse.from(documents.getFirst());
  }
}
