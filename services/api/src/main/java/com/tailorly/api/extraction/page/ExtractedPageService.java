package com.tailorly.api.extraction.page;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import com.tailorly.api.objects.storage.ObjectStorage;

import com.tailorly.api.extraction.page.dto.ExtractedPageResponse;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtractedPageService {
  private final ExtractedPageRepository extractedPageRepository;
  private final ExtractionRunRepository extractionRunRepository;
  private final OwnerAccessService ownerAccessService;
  private final ObjectStorage storage;

  @Transactional(readOnly = true)
  public String readText(UUID id) {
    ExtractedPage page = getPage(id);
    ownerAccessService.assertCanAccess(page.getExtractionRun().getBaseVersion().getOwner());
    return new String(storage.get(page.getTextObject().getStorageKey()), StandardCharsets.UTF_8);
  }

  @Transactional(readOnly = true)
  public ExtractedPageResponse findById(UUID id) {
    ExtractedPage page = getPage(id);
    ownerAccessService.assertCanAccess(page.getExtractionRun().getBaseVersion().getOwner());

    return ExtractedPageResponse.from(page);
  }

  @Transactional(readOnly = true)
  public List<ExtractedPageResponse> findAllByRunId(UUID runId) {
    ExtractionRun run = getRun(runId);
    ownerAccessService.assertCanAccess(run.getBaseVersion().getOwner());

    return extractedPageRepository.findAllByExtractionRunId(runId).stream()
        .map(ExtractedPageResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExtractedPageResponse findByTextHash(String textHash) {
    ownerAccessService.assertAdmin();

    List<ExtractedPage> pages = extractedPageRepository.findAllByTextHash(textHash);

    if (pages.isEmpty()) {
      throw new ResourceNotFoundException(ErrorCodes.ExtractedPage.NOT_FOUND);
    }

    if (pages.size() > 1) {
      throw new ResourceAlreadyExistsException(ErrorCodes.ExtractedPage.DUPLICATE_PAGE_NUMBER);
    }

    return ExtractedPageResponse.from(pages.getFirst());
  }

  private ExtractedPage getPage(UUID id) {
    return extractedPageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractedPage.NOT_FOUND));
  }

  private ExtractionRun getRun(UUID id) {
    return extractionRunRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractionRun.NOT_FOUND));
  }
}
