package com.tailorly.api.extraction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tailorly.api.extraction.interpreting.CurriculumInterpreter;
import com.tailorly.api.extraction.interpreting.ExtractedTextPage;
import com.tailorly.api.extraction.interpreting.FactProposal;
import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.extraction.processing.ExtractionFailure;
import com.tailorly.api.extraction.processing.PdfExtractor;
import com.tailorly.api.objects.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consome a fila de extração em segundo plano: a cada {@code tailorly.extraction.poll-delay-ms}
 * (padrão 2s), reivindica uma execução pendente via {@link ExtractionQueue#claim} e processa o
 * PDF fora de transação de banco e, após publicar as páginas, chama o interpretador configurado.
 *
 * <p>Desligado definindo {@code tailorly.extraction.enabled=false} (útil em testes), mantendo a
 * fila intacta sem processá-la.</p>
 *
 * <h2>Compensação em caso de falha</h2>
 * <p>{@link #process} grava o texto de cada página no storage antes de persistir qualquer
 * registro no banco. Se uma falha ocorrer depois de algumas páginas já gravadas, o worker
 * tenta remover esses objetos órfãos ({@code writtenKeys}) para não acumular lixo no bucket —
 * mas apenas depois de confirmar com {@link ExtractionQueue#completed} que a execução
 * realmente não foi publicada. Isso evita apagar páginas válidas quando a falha foi apenas na
 * conexão usada para verificar o resultado do commit (a transação pode ter sido confirmada no
 * banco mesmo que a resposta não tenha chegado ao worker). Se o próprio banco estiver
 * indisponível nesse instante, o worker desiste da limpeza e deixa uma reconciliação manual
 * para depois, em vez de arriscar apagar dados publicados.</p>
 *
 * <p>A variável {@code stage} rastreia em qual etapa uma falha inesperada ocorreu
 * ({@code STORAGE_READ_FAILED}, {@code EXTRACTION_FAILED}, {@code STORAGE_WRITE_FAILED} ou
 * {@code EXTRACTION_PERSIST_FAILED}), usada como código de erro quando a exceção não é um
 * {@link ExtractionFailure} com código próprio (por exemplo, {@code PDF_ENCRYPTED} ou
 * {@code OCR_UNAVAILABLE} vindos de {@link PdfExtractor}).</p>
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "tailorly.extraction.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ExtractionWorker {
  private static final Logger log = LoggerFactory.getLogger(ExtractionWorker.class);
  private final ExtractionQueue queue;
  private final PdfExtractor extractor;
  private final ObjectStorage storage;
  private final CurriculumInterpreter interpreter;
  private final ExtractionFinalizationService finalization;

  @Scheduled(fixedDelayString = "${tailorly.extraction.poll-delay-ms:2000}")
  public void poll() {
    queue.claim().ifPresent(this::process);
  }

  /**
   * Processa uma execução reivindicada; veja a documentação da classe para o fluxo completo.
   */
  void process(ExtractionQueue.Work work) {
    List<String> writtenKeys = new ArrayList<>();
    boolean pagesPersisted = false;
    String stage = "STORAGE_READ_FAILED";

    try {
      // Uma fila criada com outra configuração do pipeline (extrator, OCR, etc.) é inválida
      // para o worker atual; falhar cedo permite uma nova solicitação com a configuração atual.
      if (!work.fingerprint().equals(extractor.fingerprint())) {
        throw new ExtractionFailure("PIPELINE_CHANGED");
      }

      byte[] pdf = storage.get(work.originalKey());
      stage = "EXTRACTION_FAILED";
      var extracted = extractor.extract(pdf);

      List<ExtractionQueue.StoredPage> results = new ArrayList<>();
      stage = "STORAGE_WRITE_FAILED";
      for (var page : extracted) {
        byte[] text = page.text().getBytes(StandardCharsets.UTF_8);
        String key = "owners/%s/extractions/%s/pages/%d.txt"
            .formatted(work.ownerId(), work.id(), page.number());

        writtenKeys.add(key);
        storage.put(key, text, "text/plain; charset=utf-8");
        results.add(new ExtractionQueue.StoredPage(page.number(), page.method(), key,
            PdfExtractor.hash(text), text.length));
      }

      stage = "EXTRACTION_PERSIST_FAILED";
      List<ExtractedPage> persistedPages = queue.persistPages(work.id(), results);
      pagesPersisted = true;

      stage = "AI_INTERPRETATION_FAILED";
      List<FactProposal> proposals = interpret(work.ownerId(), extracted, persistedPages);

      stage = "FACT_FINALIZATION_FAILED";
      int persistedCount = finalization.finalizeExtraction(work.id(), proposals);
      log.info("Interpretação da execução {} persistiu {} propostas para revisão", work.id(), persistedCount);
    } catch (RuntimeException ex) {
      // Um erro de conexão durante o commit pode ter ocorrido após a confirmação.
      // Só compensar objetos quando o banco confirmar que não foram publicados.
      try {
        if (queue.completed(work.id()))
          return;
        pagesPersisted = pagesPersisted || queue.hasPersistedPages(work.id());
      } catch (RuntimeException unavailable) {
        log.warn("Banco indisponível; limpeza da execução {} requer reconciliação", work.id());
        return;
      }

      if (!pagesPersisted) {
        for (String key : writtenKeys) {
          try {
            storage.delete(key);
          } catch (RuntimeException cleanupError) {
            log.warn("Falha ao limpar objeto parcial da execução {}", work.id());
          }
        }
      }

      String code = ex instanceof ExtractionFailure ? ex.getMessage() : stage;
      queue.fail(work.id(), code);
      log.warn("Extração {} falhou: {}", work.id(), code, ex);
    }
  }

  private List<FactProposal> interpret(UUID ownerId, List<PdfExtractor.PageText> extracted,
      List<ExtractedPage> persistedPages) {
    if (interpreter == null || persistedPages == null || persistedPages.isEmpty()) {
      throw new IllegalStateException("No interpreter or extracted pages available for interpretation");
    }

    Map<Integer, PdfExtractor.PageText> extractedByNumber = extracted.stream()
        .collect(Collectors.toMap(PdfExtractor.PageText::number, Function.identity()));

    List<ExtractedTextPage> pages = persistedPages.stream()
        .map(page -> {
          var source = extractedByNumber.get(page.getPageNumber());
          return source == null ? null : new ExtractedTextPage(page.getId(), page.getPageNumber(),
              page.getExtractionMethod(), source.text());
        })
        .filter(java.util.Objects::nonNull)
        .toList();

    if (pages.isEmpty()) {
      throw new IllegalStateException("No extracted page text available for interpretation");
    }

    List<FactProposal> proposals = interpreter.interpret(ownerId, pages);
    if (proposals == null) {
      throw new IllegalStateException("Curriculum interpreter returned no result");
    }
    return proposals;
  }
}
