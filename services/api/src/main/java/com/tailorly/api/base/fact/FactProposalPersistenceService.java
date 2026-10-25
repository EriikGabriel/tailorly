package com.tailorly.api.base.fact;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.tailorly.api.base.fact.evidence.FactEvidence;
import com.tailorly.api.base.fact.evidence.FactEvidenceRepository;
import com.tailorly.api.base.fact.group.FactRecord;
import com.tailorly.api.base.fact.group.FactRecordRepository;
import com.tailorly.api.base.review.ReviewIssue;
import com.tailorly.api.base.review.ReviewIssueRepository;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.extraction.interpreting.FactProposal;
import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FactProposalPersistenceService {
  private static final String PENDING = "pendente";
  private static final String AI_ORIGIN = "ai";

  private final ExtractionRunRepository runs;
  private final BaseVersionRepository versions;
  private final ExtractedPageRepository pages;
  private final FactRecordRepository records;
  private final FactRepository facts;
  private final FactEvidenceRepository evidence;
  private final ReviewIssueRepository issues;
  private final ObjectStorage storage;

  @Transactional
  public int persist(UUID runId, List<FactProposal> proposals) {
    // Uma lista vazia não precisa consultar nem alterar o banco.
    if (proposals == null || proposals.isEmpty())
      return 0;

    // Os locks serializam gravações desta execução e impedem alterações após a ativação da versão.
    ExtractionRun run = runs.findForUpdate(runId)
        .orElseThrow(() -> new IllegalArgumentException("Extraction run not found"));
    BaseVersion version = versions.findByIdForUpdate(run.getBaseVersion().getId())
        .orElseThrow(() -> new IllegalArgumentException("Base version not found"));
    if (!"CANDIDATE".equals(version.getStatus()) && !"EXTRACTED".equals(version.getStatus())) {
      throw new IllegalStateException("Facts can only be proposed for a candidate or extracted version");
    }

    Map<String, FactRecord> recordsByCategory = new HashMap<>();
    List<FactRecord> versionRecords = records.findAllByBaseVersionIdOrderByDisplayOrderAsc(version.getId());
    // computeIfAbsent cria categorias novas; o contador preserva uma ordem crescente na versão.
    AtomicInteger nextDisplayOrder = new AtomicInteger(
        versionRecords.stream().mapToInt(FactRecord::getDisplayOrder).max().orElse(-1) + 1);
    for (FactRecord factRecord : versionRecords) {
      recordsByCategory.putIfAbsent(factRecord.getCategory(), factRecord);
    }

    // A chave por página, campo e trecho torna uma repetição do mesmo run idempotente.
    Set<ProposalIdentity> existingProposals = existingProposalIdentities(runId);
    int persistedCount = 0;
    for (FactProposal proposal : proposals) {
      ExtractedPage page = validateAndGetPage(run, proposal);
      ProposalIdentity identity = new ProposalIdentity(page.getId(), proposal.fieldKey().trim(),
          proposal.sourceExcerpt());
      if (!existingProposals.add(identity))
        continue;

      String category = proposal.category().trim();
      FactRecord factRecord = recordsByCategory.computeIfAbsent(category,
          key -> records.save(FactRecord.builder()
              .baseVersion(version)
              .category(key)
              .displayOrder(nextDisplayOrder.getAndIncrement())
              .build()));

      Fact fact = facts.save(Fact.builder()
          .factRecord(factRecord)
          .extractionRun(run)
          .fieldKey(proposal.fieldKey().trim())
          .extractedValue(proposal.extractedValue())
          .origin(AI_ORIGIN)
          .reviewState(PENDING)
          .build());

      // Evidência e fato são gravados na mesma transação para não publicar um sem o outro.
      evidence.save(FactEvidence.builder()
          .fact(fact)
          .extractedPage(page)
          .sourceExcerpt(proposal.sourceExcerpt())
          .sourceLocator(Map.of(
              "type", "text-offset",
              "start", proposal.excerptStart(),
              "end", proposal.excerptEnd()))
          .build());

      // Cada sugestão bloqueia a aprovação até uma pessoa confirmar, corrigir ou omitir o valor.
      issues.save(ReviewIssue.builder()
          .baseVersion(version)
          .fact(fact)
          .category(category)
          .reason("Revise este fato extraído antes de usá-lo.")
          .blocking(true)
          .status("PENDING")
          .questionAndChoices(Map.of(
              "question", "Confirme, corrija ou omita este valor.",
              "choices", List.of("confirmado", "corrigido", "omitido")))
          .build());
      persistedCount++;
    }
    return persistedCount;
  }

  private Set<ProposalIdentity> existingProposalIdentities(UUID runId) {
    Set<ProposalIdentity> identities = new HashSet<>();
    // Considera evidências já persistidas para não duplicar fatos se esta operação for repetida.
    for (Fact fact : facts.findAllByExtractionRunId(runId)) {
      for (FactEvidence item : evidence.findAllByFactId(fact.getId())) {
        identities.add(new ProposalIdentity(item.getExtractedPage().getId(), fact.getFieldKey(),
            item.getSourceExcerpt()));
      }
    }
    return identities;
  }

  private ExtractedPage validateAndGetPage(ExtractionRun run, FactProposal proposal) {
    // Os campos vindos do modelo são dados não confiáveis e também precisam caber no esquema.
    if (proposal == null || proposal.category() == null || proposal.category().isBlank()
        || proposal.category().length() > 255 || proposal.fieldKey() == null
        || proposal.fieldKey().isBlank() || proposal.fieldKey().length() > 255
        || proposal.extractedValue() == null || proposal.sourceExcerpt() == null
        || proposal.sourceExcerpt().isBlank() || proposal.extractedPageId() == null) {
      throw new IllegalArgumentException("AI fact proposal is incomplete");
    }

    ExtractedPage page = pages.findById(proposal.extractedPageId())
        .orElseThrow(() -> new IllegalArgumentException("Evidence page not found"));
    if (!run.getId().equals(page.getExtractionRun().getId())) {
      throw new IllegalArgumentException("Evidence page does not belong to the extraction run");
    }

    // Revalida os offsets contra o texto original guardado; não confia na posição sugerida pelo modelo.
    String pageText = new String(storage.get(page.getTextObject().getStorageKey()), StandardCharsets.UTF_8);
    int start = proposal.excerptStart();
    int end = proposal.excerptEnd();
    if (start < 0 || end > pageText.length() || start >= end
        || !pageText.substring(start, end).equals(proposal.sourceExcerpt())) {
      throw new IllegalArgumentException("AI fact proposal evidence does not match extracted page text");
    }
    return page;
  }

  private record ProposalIdentity(UUID pageId, String fieldKey, String excerpt) {
  }
}
