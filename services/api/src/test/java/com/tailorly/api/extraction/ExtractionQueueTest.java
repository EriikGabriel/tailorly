package com.tailorly.api.extraction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tailorly.api.base.document.BaseDocument;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.extraction.processing.ExtractionFailure;
import com.tailorly.api.extraction.processing.PdfExtractor;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.owner.Owner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractionQueueTest {
  private final ExtractionRunRepository runs = mock(ExtractionRunRepository.class);
  private final ExtractedPageRepository pages = mock(ExtractedPageRepository.class);
  private final StoredObjectRepository objects = mock(StoredObjectRepository.class);
  private final PdfExtractor extractor = mock(PdfExtractor.class);
  private final ExtractionQueue queue = new ExtractionQueue(runs, pages, objects, extractor);
  private BaseVersion version;

  @BeforeEach
  void setup() {
    Owner owner = new Owner();
    owner.setId(UUID.randomUUID());
    owner.setActiveSnapshotId(UUID.randomUUID());
    version = BaseVersion.builder().id(UUID.randomUUID()).owner(owner)
        .document(new BaseDocument()).status("CANDIDATE").build();
  }

  @Test
  @DisplayName("reenvio reutiliza execução concluída sem criar outra")
  void reuse() {
    ExtractionRun run = ExtractionRun.builder().status("COMPLETED").build();
    when(extractor.fingerprint()).thenReturn("pipeline");
    when(runs.findFirstByBaseVersionIdAndPipelineFingerprintAndStatusIn(eq(version.getId()), eq("pipeline"), anyList()))
        .thenReturn(Optional.of(run));
    assertSame(run, queue.enqueue(version));
    verify(runs, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("concluir persiste páginas e preserva snapshot e revisão existentes")
  void complete() {
    UUID id = UUID.randomUUID();
    UUID snapshot = version.getOwner().getActiveSnapshotId();
    version.setStatus("ACTIVE");
    version.setReviewRevision(4);
    ExtractionRun run = ExtractionRun.builder().id(id).baseVersion(version).status("PROCESSING").build();
    when(runs.findForUpdate(id)).thenReturn(Optional.of(run));
    when(objects.save(any())).thenAnswer(i -> i.getArgument(0));
    when(pages.save(any())).thenAnswer(i -> i.getArgument(0));
    queue.persistPages(id, List.of(new ExtractionQueue.StoredPage(1, "DIRECT", "key", "hash", 100)));
    assertEquals("PROCESSING", run.getStatus());
    when(pages.existsByExtractionRunId(id)).thenReturn(true);
    queue.complete(id);
    assertEquals("COMPLETED", run.getStatus());
    assertNotNull(run.getFinishedAt());
    assertEquals("ACTIVE", version.getStatus());
    assertEquals(4, version.getReviewRevision());
    assertEquals(snapshot, version.getOwner().getActiveSnapshotId());
    verify(pages).save(argThat(page -> page.getPageNumber() == 1 && page.getTextHash().equals("hash")));
  }

  @Test
  @DisplayName("worker atrasado não conclui uma execução já expirada")
  void staleWorker() {
    UUID id = UUID.randomUUID();
    when(runs.findForUpdate(id)).thenReturn(Optional.of(ExtractionRun.builder().status("FAILED").build()));
    assertThrows(ExtractionFailure.class,
        () -> queue.complete(id));
    verifyNoInteractions(objects, pages);
  }
}
