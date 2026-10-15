package com.tailorly.api.extraction;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.BaseVersion;
import com.tailorly.api.extraction.dto.CreateExtractedPageRequest;
import com.tailorly.api.extraction.dto.ExtractedPageResponse;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtractedPageService {
  private final ExtractedPageRepository extractedPageRepository;
  private final ExtractionRunRepository extractionRunRepository;
  private final StoredObjectRepository storedObjectRepository;
  private final OwnerAccessService ownerAccessService;

  @Transactional
  public ExtractedPageResponse create(CreateExtractedPageRequest request) {
    ExtractionRun run = getRun(request.extractionRunId());
    BaseVersion version = run.getBaseVersion();

    ownerAccessService.assertCanAccess(version.getOwner());

    if (request.pageNumber() != null
        && extractedPageRepository.existsByExtractionRunIdAndPageNumber(
        run.getId(), request.pageNumber())) {
      throw new ResourceAlreadyExistsException(ErrorCodes.ExtractedPage.HASH_NOT_UNIQUE);
    }

    StoredObject textObject = storedObjectRepository.findById(request.textObjectId())
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.StoredObject.NOT_FOUND));

    if (!version.getOwner().getId().equals(textObject.getOwner().getId())) {
      throw new AccessDeniedException(ErrorCodes.ExtractedPage.TEXT_OBJECT_OWNER_MISMATCH);
    }

    ExtractedPage page = ExtractedPage.builder()
        .extractionRun(run)
        .pageNumber(request.pageNumber())
        .extractionMethod(request.extractionMethod())
        .textObject(textObject)
        .textHash(request.textHash())
        .build();

    return ExtractedPageResponse.from(extractedPageRepository.save(page));
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
