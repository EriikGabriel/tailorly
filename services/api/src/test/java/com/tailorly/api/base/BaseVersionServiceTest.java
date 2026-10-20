package com.tailorly.api.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseVersionServiceTest {
  @Mock BaseVersionRepository repository;
  @Mock OwnerAccessService ownerAccessService;
  @InjectMocks BaseVersionService service;

  private Owner owner;
  private BaseDocument document;

  @BeforeEach
  void setup() {
    owner = new Owner();
    owner.setId(UUID.randomUUID());
    owner.setActiveSnapshotId(UUID.randomUUID());
    document = new BaseDocument();
    document.setId(UUID.randomUUID());
    document.setOwner(owner);
  }

  @Test
  @DisplayName("o primeiro upload cria a candidata com número 1")
  void createsFirstCandidateWithVersionOne() {
    when(repository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId()))
        .thenReturn(Optional.empty());
    when(repository.findFirstByOwnerIdOrderByVersionNumberDesc(owner.getId()))
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.findOrCreateForUpload(document);

    assertFalse(result.reused());
    assertEquals(1, result.version().getVersionNumber());
    assertEquals(0, result.version().getReviewRevision());
    assertEquals(BaseVersionService.CANDIDATE, result.version().getStatus());
  }

  @Test
  @DisplayName("a nova candidata recebe o próximo número sem alterar o snapshot ativo")
  void createsCandidateWithNextOwnerNumberWithoutChangingActiveSnapshot() {
    UUID activeSnapshotId = owner.getActiveSnapshotId();
    BaseVersion previous = BaseVersion.builder().versionNumber(4).build();
    when(repository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId()))
        .thenReturn(Optional.empty());
    when(repository.findFirstByOwnerIdOrderByVersionNumberDesc(owner.getId()))
        .thenReturn(Optional.of(previous));
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.findOrCreateForUpload(document);

    assertFalse(result.reused());
    assertEquals(5, result.version().getVersionNumber());
    assertEquals(BaseVersionService.CANDIDATE, result.version().getStatus());
    assertTrue(result.version().isTemporalReviewRequired());
    assertSame(document, result.version().getDocument());
    assertEquals(activeSnapshotId, owner.getActiveSnapshotId());
  }

  @Test
  @DisplayName("o reenvio reaproveita a versão sem zerar status ou revisão")
  void reusesVersionWithoutResettingStatusOrReview() {
    BaseVersion existing = BaseVersion.builder()
        .owner(owner)
        .document(document)
        .versionNumber(2)
        .status("ACTIVE")
        .reviewRevision(3)
        .temporalReviewRequired(false)
        .build();
    UUID activeSnapshotId = owner.getActiveSnapshotId();
    when(repository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId()))
        .thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(existing)).thenReturn(existing);

    var result = service.findOrCreateForUpload(document);

    assertTrue(result.reused());
    assertSame(existing, result.version());
    assertEquals("ACTIVE", existing.getStatus());
    assertEquals(3, existing.getReviewRevision());
    assertTrue(existing.isTemporalReviewRequired());
    assertEquals(activeSnapshotId, owner.getActiveSnapshotId());
    verify(repository, never()).findFirstByOwnerIdOrderByVersionNumberDesc(any());
  }
}
