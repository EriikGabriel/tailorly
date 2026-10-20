package com.tailorly.api.extraction;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import java.util.UUID;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.extraction.page.ExtractedPageService;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ExtractionAccessTest {
  @Test
  @DisplayName("outra conta não consegue ler texto extraído no storage")
  void deniesText() {
    var pages = mock(ExtractedPageRepository.class);
    var runs = mock(ExtractionRunRepository.class);
    var access = mock(OwnerAccessService.class);
    var storage = mock(ObjectStorage.class);
    var service = new ExtractedPageService(pages, runs, access, storage);
    UUID id = UUID.randomUUID();
    Owner owner = new Owner();
    when(pages.findById(id)).thenReturn(Optional.of(ExtractedPage.builder()
        .extractionRun(ExtractionRun.builder().baseVersion(BaseVersion.builder().owner(owner).build()).build())
        .build()));
    doThrow(new AccessDeniedException("denied")).when(access).assertCanAccess(owner);
    assertThrows(AccessDeniedException.class, () -> service.readText(id));
    verifyNoInteractions(storage);
  }
}
