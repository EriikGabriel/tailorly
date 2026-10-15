package com.tailorly.api.base;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import com.tailorly.api.base.dto.BaseDocumentResponse;
import com.tailorly.api.base.dto.BaseDocumentUploadResponse;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BaseDocumentUploadService {
  private static final Logger log = LoggerFactory.getLogger(BaseDocumentUploadService.class);
  private static final long MAX_PDF_SIZE = 7L * 1024 * 1024;
  private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

  private final OwnerRepository ownerRepository;
  private final OwnerAccessService ownerAccessService;
  private final BaseDocumentRepository documentRepository;
  private final StoredObjectRepository objectRepository;
  private final ObjectStorage storage;

  @Transactional
  public BaseDocumentUploadResponse upload(UUID ownerId, MultipartFile file) {
    Owner owner = ownerRepository.findByIdForUpdate(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    ownerAccessService.assertCanAccess(owner);

    if (owner.getKind() != OwnerKindEnum.KIND_ACCOUNT) {
      throw new BadRequestException(ErrorCodes.BaseDocument.ACCOUNT_OWNER_REQUIRED);
    }

    if (file == null || file.isEmpty()) {
      throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
    }
    
    if (file.getSize() > MAX_PDF_SIZE) {
      throw new BadRequestException(ErrorCodes.BaseDocument.FILE_TOO_LARGE);
    }

    byte[] bytes = readPdf(file);
    String hash = sha256(bytes);
    var existing = documentRepository.findByOwnerIdAndContentHash(ownerId, hash);
    if (existing.isPresent()) {
      if (!storage.exists(existing.get().getOriginalObject().getStorageKey())) {
        throw new ResourceAlreadyExistsException(ErrorCodes.BaseDocument.ORIGINAL_OBJECT_MISSING);
      }
      return BaseDocumentUploadResponse.builder()
          .document(BaseDocumentResponse.from(existing.get()))
          .reused(true)
          .build();
    }

    String key = "owners/" + ownerId + "/originals/" + UUID.randomUUID() + ".pdf";
    storage.put(key, bytes, "application/pdf");
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          try {
            storage.delete(key);
          } catch (RuntimeException ex) {
            log.error("Could not remove rolled-back upload at {}", key, ex);
          }
        }
      }
    });

    StoredObject object = StoredObject.builder()
        .owner(owner)
        .purpose("BASE_ORIGINAL")
        .storageKey(key)
        .contentHash(hash)
        .mimeType("application/pdf")
        .sizeBytes((long) bytes.length)
        .deletionStatus("ACTIVE")
        .build();
    objectRepository.save(object);

    BaseDocument document = BaseDocument.builder()
        .owner(owner)
        .originalObject(object)
        .contentHash(hash)
        .originalFilename(safeFilename(file.getOriginalFilename()))
        .build();
    documentRepository.saveAndFlush(document);

    return BaseDocumentUploadResponse.builder()
        .document(BaseDocumentResponse.from(document))
        .reused(false)
        .build();
  }

  private byte[] readPdf(MultipartFile file) {
    try {
      byte[] bytes = file.getBytes();
      if (bytes.length == 0 || bytes.length > MAX_PDF_SIZE) {
        throw new BadRequestException(bytes.length == 0
            ? ErrorCodes.BaseDocument.INVALID_PDF : ErrorCodes.BaseDocument.FILE_TOO_LARGE);
      }
      if (bytes.length < PDF_HEADER.length) {
        throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
      }
      for (int i = 0; i < PDF_HEADER.length; i++) {
        if (bytes[i] != PDF_HEADER[i]) {
          throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
        }
      }
      return bytes;
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read uploaded PDF", ex);
    }
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private String safeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return "curriculo.pdf";
    }
    String basename = filename.replace('\\', '/');
    basename = basename.substring(basename.lastIndexOf('/') + 1);
    return basename.isBlank() ? "curriculo.pdf" : basename.substring(0, Math.min(255, basename.length()));
  }
}
