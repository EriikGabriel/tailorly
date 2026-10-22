package com.tailorly.api.base.fact.evidence;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import com.tailorly.api.base.fact.Fact;
import com.tailorly.api.base.fact.FactRepository;
import com.tailorly.api.base.fact.evidence.dto.CreateFactEvidenceRequest;
import com.tailorly.api.base.fact.evidence.dto.FactEvidenceResponse;
import com.tailorly.api.base.fact.evidence.dto.UpdateEvidenceLocatorRequest;
import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FactEvidenceService {
  private final FactEvidenceRepository evidenceRepository;
  private final FactRepository factRepository;
  private final ExtractedPageRepository pageRepository;
  private final OwnerAccessService access;
  private final ObjectStorage storage;

  @Transactional
  public FactEvidenceResponse create(CreateFactEvidenceRequest request) {
    Fact fact = getFact(request.factId());
    ExtractedPage page = getPage(request.extractedPageId());
    var version = fact.getFactRecord().getBaseVersion();
    access.assertCanAccess(version.getOwner());
    var pageVersion = page.getExtractionRun().getBaseVersion();
    if (!version.getId().equals(pageVersion.getId())
        || (fact.getExtractionRun() != null
        && !fact.getExtractionRun().getId().equals(page.getExtractionRun().getId()))) {
      throw new BadRequestException(ErrorCodes.FactEvidence.PAGE_VERSION_MISMATCH);
    }
    String pageText = new String(storage.get(page.getTextObject().getStorageKey()), StandardCharsets.UTF_8);
    if (!pageText.contains(request.sourceExcerpt())) {
      throw new BadRequestException(ErrorCodes.FactEvidence.EXCERPT_NOT_IN_SOURCE);
    }
    return FactEvidenceResponse.from(evidenceRepository.save(FactEvidence.builder()
        .fact(fact)
        .extractedPage(page)
        .sourceExcerpt(request.sourceExcerpt())
        .sourceLocator(request.sourceLocator())
        .boundingBox(request.boundingBox())
        .build()));
  }

  @Transactional(readOnly = true)
  public FactEvidenceResponse findById(UUID id) {
    FactEvidence evidence = getEvidence(id);
    access.assertCanAccess(evidence.getFact().getFactRecord().getBaseVersion().getOwner());
    return FactEvidenceResponse.from(evidence);
  }

  @Transactional(readOnly = true)
  public List<FactEvidenceResponse> findByFactId(UUID factId) {
    Fact fact = getFact(factId);
    access.assertCanAccess(fact.getFactRecord().getBaseVersion().getOwner());
    return evidenceRepository.findAllByFactId(factId).stream()
        .map(FactEvidenceResponse::from).toList();
  }

  @Transactional
  public FactEvidenceResponse updateLocator(UUID id, UpdateEvidenceLocatorRequest request) {
    FactEvidence evidence = getEvidence(id);
    access.assertCanAccess(evidence.getFact().getFactRecord().getBaseVersion().getOwner());
    evidence.setSourceLocator(request.sourceLocator());
    return FactEvidenceResponse.from(evidenceRepository.save(evidence));
  }

  private Fact getFact(UUID id) {
    return factRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Fact.NOT_FOUND));
  }

  private ExtractedPage getPage(UUID id) {
    return pageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.ExtractedPage.NOT_FOUND));
  }

  private FactEvidence getEvidence(UUID id) {
    return evidenceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.FactEvidence.NOT_FOUND));
  }
}
