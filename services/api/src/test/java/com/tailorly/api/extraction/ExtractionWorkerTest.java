package com.tailorly.api.extraction;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import com.tailorly.api.extraction.processing.ExtractionFailure;
import com.tailorly.api.extraction.processing.PdfExtractor;
import com.tailorly.api.objects.storage.ObjectStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractionWorkerTest {
  private final ExtractionQueue queue = mock(ExtractionQueue.class);
  private final PdfExtractor extractor = mock(PdfExtractor.class);
  private final ObjectStorage storage = mock(ObjectStorage.class);
  private final ExtractionWorker worker = new ExtractionWorker(queue, extractor, storage);
  private final ExtractionQueue.Work work = new ExtractionQueue.Work(UUID.randomUUID(), UUID.randomUUID(), "original", "pipeline");

  @Test
  @DisplayName("falha ao persistir remove textos parciais e registra erro")
  void rollback() {
    when(extractor.fingerprint()).thenReturn("pipeline");
    when(storage.get("original")).thenReturn(new byte[]{1});
    when(extractor.extract(any())).thenReturn(List.of(new PdfExtractor.PageText(1, "DIRECT", "texto")));
    doThrow(new RuntimeException()).when(queue).complete(eq(work.id()), anyList());
    worker.process(work);
    verify(storage).delete("owners/%s/extractions/%s/pages/1.txt".formatted(work.ownerId(), work.id()));
    verify(queue).fail(work.id(), "EXTRACTION_PERSIST_FAILED");
  }

  @Test
  @DisplayName("falha no OCR não publica páginas nem conclui a execução")
  void ocrFailure() {
    when(extractor.fingerprint()).thenReturn("pipeline");
    when(storage.get("original")).thenReturn(new byte[]{1});
    when(extractor.extract(any())).thenThrow(new ExtractionFailure("OCR_UNAVAILABLE"));
    worker.process(work);
    verify(storage, never()).put(anyString(), any(), anyString());
    verify(queue, never()).complete(any(), anyList());
    verify(queue).fail(work.id(), "OCR_UNAVAILABLE");
  }
}
