package com.tailorly.api.extraction;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.extraction.page.ExtractedPageRepository;
import com.tailorly.api.extraction.processing.ExtractionFailure;
import com.tailorly.api.extraction.processing.PdfExtractor;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.extraction.run.ExtractionRunRepository;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o ciclo de vida de uma {@link ExtractionRun}: enfileirar, reivindicar para
 * processamento ({@link ExtractionWorker}), concluir com as páginas extraídas ou falhar.
 *
 * <p>Os métodos transacionais implementam as regras de fila descritas no README da API:</p>
 * <ul>
 *   <li>{@link #enqueue} reaproveita uma execução {@code QUEUED}/{@code PROCESSING}/
 *       {@code COMPLETED} com o mesmo fingerprint do pipeline, evitando duplicar trabalho em
 *       reenvios; exige que o chamador já tenha o lock do owner
 *       ({@link Propagation#MANDATORY}).</li>
 *   <li>{@link #claim} expira execuções {@code PROCESSING} há mais de 10 minutos
 *       ({@code WORKER_TIMEOUT}) antes de reservar a próxima da fila com
 *       {@code FOR UPDATE SKIP LOCKED}, permitindo vários workers sem duplicar trabalho.</li>
 *   <li>{@link #persistPages} publica as páginas sem concluir a execução; depois da interpretação
 *       e persistência dos fatos, {@link #complete} encerra a execução e avança a versão candidata
 *       para {@code EXTRACTED}, sem alterar versões já ativas.</li>
 *   <li>{@link #fail} e {@link #completed} apoiam a compensação de objetos parciais no
 *       storage feita pelo {@link ExtractionWorker} quando a transação final falha.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ExtractionQueue {
  private final ExtractionRunRepository runs;
  private final ExtractedPageRepository pages;
  private final StoredObjectRepository objects;
  private final PdfExtractor extractor;
  private final AnonymousQuotaService anonymousQuota;

  // Chamadores mantêm o lock do owner até a transação terminar.
  @Transactional(propagation = Propagation.MANDATORY)
  public ExtractionRun enqueue(BaseVersion version) {
    String fingerprint = extractor.fingerprint();
    return runs.findFirstByBaseVersionIdAndPipelineFingerprintAndStatusIn(version.getId(), fingerprint,
        List.of("QUEUED", "PROCESSING", "COMPLETED"))
        .orElseGet(() -> {
          // Só uma execução nova consome o limite de processamento anônimo; reaproveitar não consome.
          anonymousQuota.consumeExtraction(version.getOwner());
          return runs.saveAndFlush(ExtractionRun.builder().baseVersion(version)
            .pipelineFingerprint(fingerprint).pipelineVersions(extractor.versions())
            .status("QUEUED").build());
        });
  }

  /**
   * Marca como {@code FAILED}/{@code WORKER_TIMEOUT} execuções {@code PROCESSING} há mais de
   * 10 minutos e reserva a próxima execução {@code QUEUED} com {@code FOR UPDATE SKIP LOCKED},
   * para que vários workers possam consultar a fila sem reservar a mesma execução.
   */
  @Transactional
  public Optional<Work> claim() {
    Instant now = Instant.now();
    runs.expireStalled(now.minus(Duration.ofMinutes(10)), now);

    return runs.findNextQueued().map(run -> {
      run.setStatus("PROCESSING");
      run.setStartedAt(now);
      var version = run.getBaseVersion();
      return new Work(run.getId(), version.getOwner().getId(),
          version.getDocument().getOriginalObject().getStorageKey(), run.getPipelineFingerprint());
    });
  }

  /**
   * Publica as páginas extraídas, mantendo a execução em {@code PROCESSING} para interpretação.
   *
   * <p>Rejeita uma execução que não esteja mais {@code PROCESSING} ({@code EXTRACTION_EXPIRED}),
   * o que impede um worker atrasado de publicar resultados após o timeout de {@link #claim}.
   * Um documento totalmente sem texto útil chega aqui com {@code results} vazio e falha com
   * {@code PDF_NO_TEXT} em vez de publicar uma versão sem conteúdo útil.</p>
   */
  @Transactional
  public List<ExtractedPage> persistPages(UUID runId, List<StoredPage> results) {
    ExtractionRun run = runs.findForUpdate(runId).orElseThrow();
    if (!"PROCESSING".equals(run.getStatus())) throw new ExtractionFailure("EXTRACTION_EXPIRED");
    if (results.isEmpty()) throw new ExtractionFailure("PDF_NO_TEXT");

    var owner = run.getBaseVersion().getOwner();

    List<ExtractedPage> persistedPages = new ArrayList<>();

    for (StoredPage result : results) {
      StoredObject object = objects.save(StoredObject.builder().owner(owner)
          .purpose("BASE_EXTRACTED_PAGE_TEXT").storageKey(result.key()).contentHash(result.hash())
          .mimeType("text/plain; charset=utf-8").sizeBytes(result.bytes())
          .expiresAt(owner.getExpiresAt()).deletionStatus("ACTIVE").build());
      persistedPages.add(pages.save(ExtractedPage.builder().extractionRun(run).pageNumber(result.number())
          .extractionMethod(result.method()).textObject(object).textHash(result.hash()).build()));
    }

    run.getBaseVersion().getDocument().setPageCount(results.size());

    return List.copyOf(persistedPages);
  }

  /** Conclui apenas após interpretação e persistência bem-sucedidas dos fatos. */
  @Transactional
  public void complete(UUID runId) {
    ExtractionRun run = runs.findForUpdate(runId).orElseThrow();
    if (!"PROCESSING".equals(run.getStatus())) throw new ExtractionFailure("EXTRACTION_EXPIRED");
    if (!pages.existsByExtractionRunId(runId)) throw new ExtractionFailure("PDF_NO_TEXT");

    // Só avança CANDIDATE -> EXTRACTED depois que o worker finaliza a interpretação.
    if ("CANDIDATE".equals(run.getBaseVersion().getStatus())) {
      run.getBaseVersion().setStatus("EXTRACTED");
    }
    run.setStatus("COMPLETED");
    run.setFinishedAt(Instant.now());
    run.setErrorCode(null);
  }

  @Transactional
  public void fail(UUID runId, String code) {
    runs.findForUpdate(runId).filter(run -> "PROCESSING".equals(run.getStatus())).ifPresent(run -> {
      run.setStatus("FAILED");
      run.setErrorCode(code);
      run.setFinishedAt(Instant.now());
    });
  }

  /**
   * Usado pelo {@link ExtractionWorker} para decidir, depois de uma falha de conexão durante o
   * commit de {@link #complete}, se os objetos parciais já gravados no storage podem ser
   * removidos com segurança ou se a execução pode ter sido publicada mesmo assim.
   */
  @Transactional(readOnly = true)
  public boolean completed(UUID runId) {
    return runs.findById(runId).map(run -> "COMPLETED".equals(run.getStatus())).orElse(false);
  }

  @Transactional(readOnly = true)
  public boolean hasPersistedPages(UUID runId) {
    return pages.existsByExtractionRunId(runId);
  }

  /** Dados mínimos para o worker processar uma execução fora da transação de reserva. */
  public record Work(UUID id, UUID ownerId, String originalKey, String fingerprint) {}

  /** Resultado de uma página processada, pronto para ser persistido em {@link #complete}. */
  public record StoredPage(int number, String method, String key, String hash, long bytes) {}
}
