package com.tailorly.api.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class BaseDocumentUploadServiceTest {
  @Mock OwnerRepository ownerRepository;
  @Mock OwnerAccessService ownerAccessService;
  @Mock BaseDocumentRepository documentRepository;
  @Mock StoredObjectRepository objectRepository;
  @Mock ObjectStorage storage;
  @InjectMocks BaseDocumentUploadService service;

  private final UUID ownerId = UUID.randomUUID();
  private Owner owner;

  @BeforeEach
  void setup() {
    owner = new Owner();
    owner.setId(ownerId);
    owner.setKind(OwnerKindEnum.KIND_ACCOUNT);
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void uploadsBytesAndRemovesObjectIfDatabaseTransactionRollsBack() {
    byte[] pdf = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
    var file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf);
    when(ownerRepository.findByIdForUpdate(ownerId)).thenReturn(Optional.of(owner));
    when(documentRepository.findByOwnerIdAndContentHash(eq(ownerId), any()))
        .thenReturn(Optional.empty());
    when(documentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
      BaseDocument document = invocation.getArgument(0);
      document.setId(UUID.randomUUID());
      return document;
    });

    var response = service.upload(ownerId, file);

    assertFalse(response.reused());
    assertEquals(ownerId, response.document().ownerId());
    var key = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(storage).put(key.capture(), eq(pdf), eq("application/pdf"));
    assertTrue(key.getValue().startsWith("owners/" + ownerId + "/originals/"));
    TransactionSynchronizationManager.getSynchronizations().forEach(
        synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verify(storage).delete(key.getValue());
  }

  @Test
  void reusesExistingDocumentWithinSameOwnerWithoutUploadingAgain() {
    byte[] pdf = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
    var file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf);
    StoredObject object = new StoredObject();
    object.setId(UUID.randomUUID());
    object.setStorageKey("owners/" + ownerId + "/originals/existing.pdf");
    BaseDocument document = new BaseDocument();
    document.setId(UUID.randomUUID());
    document.setOwner(owner);
    document.setOriginalObject(object);
    when(ownerRepository.findByIdForUpdate(ownerId)).thenReturn(Optional.of(owner));
    when(documentRepository.findByOwnerIdAndContentHash(eq(ownerId), any()))
        .thenReturn(Optional.of(document));
    when(storage.exists(object.getStorageKey())).thenReturn(true);

    var response = service.upload(ownerId, file);

    assertTrue(response.reused());
    assertEquals(document.getId(), response.document().id());
    verify(storage, never()).put(any(), any(), any());
  }

  @Test
  void doesNotReuseMetadataWhenOriginalFileIsMissing() {
    byte[] pdf = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
    var file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf);
    StoredObject object = new StoredObject();
    object.setStorageKey("owners/" + ownerId + "/originals/missing.pdf");
    BaseDocument document = new BaseDocument();
    document.setOwner(owner);
    document.setOriginalObject(object);
    when(ownerRepository.findByIdForUpdate(ownerId)).thenReturn(Optional.of(owner));
    when(documentRepository.findByOwnerIdAndContentHash(eq(ownerId), any()))
        .thenReturn(Optional.of(document));

    assertThrows(ResourceAlreadyExistsException.class, () -> service.upload(ownerId, file));
    verify(storage, never()).put(any(), any(), any());
  }
}
