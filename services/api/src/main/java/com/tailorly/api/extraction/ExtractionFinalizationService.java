package com.tailorly.api.extraction;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.FactProposalPersistenceService;
import com.tailorly.api.extraction.interpreting.FactProposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExtractionFinalizationService {
  private final FactProposalPersistenceService proposalPersistence;
  private final ExtractionQueue queue;

  @Transactional
  public int finalizeExtraction(UUID runId, List<FactProposal> proposals) {
    int persistedCount = proposalPersistence.persist(runId, proposals);
    queue.complete(runId);
    return persistedCount;
  }
}
