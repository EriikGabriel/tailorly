package com.tailorly.api.session;

import java.io.IOException;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerKindEnum;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limites de tamanho, páginas, uploads, processamentos e gerações do acesso anônimo. Todos os
 * métodos de consumo são no-op para owners de conta, então os serviços de domínio podem chamá-los
 * sem ramificar por tipo de owner. Os contadores ficam na sessão e são incrementados de forma
 * atômica, só abaixo do limite.
 */
@Service
@RequiredArgsConstructor
public class AnonymousQuotaService {
  private final AnonymousSessionRepository sessions;
  private final AnonymousProperties properties;

  public static boolean isAnonymous(Owner owner) {
    return owner.getKind() == OwnerKindEnum.KIND_ANONYMOUS;
  }

  public void assertFileSize(long sizeBytes) {
    if (sizeBytes > properties.maxFileBytes()) {
      throw new BadRequestException(ErrorCodes.Anonymous.FILE_TOO_LARGE);
    }
  }

  /** Conta as páginas do PDF e rejeita documentos acima do limite; devolve a contagem. */
  public int assertPageLimit(byte[] pdf) {
    int pages;
    try (PDDocument document = Loader.loadPDF(pdf)) {
      pages = document.getNumberOfPages();
    } catch (IOException exception) {
      throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
    }
    if (pages > properties.maxPages()) {
      throw new BadRequestException(ErrorCodes.Anonymous.PAGE_LIMIT_EXCEEDED);
    }
    return pages;
  }

  @Transactional
  public void consumeUpload(Owner owner) {
    consume(owner, properties.maxUploads(), sessions::incrementUploads,
        ErrorCodes.Anonymous.UPLOAD_LIMIT_REACHED);
  }

  @Transactional
  public void consumeExtraction(Owner owner) {
    consume(owner, properties.maxExtractions(), sessions::incrementExtractions,
        ErrorCodes.Anonymous.EXTRACTION_LIMIT_REACHED);
  }

  /** Limite de gerações de currículo (PDF e Markdown) do visitante; reaproveitar uma geração idêntica não consome. */
  @Transactional
  public void consumeGeneration(Owner owner) {
    consume(owner, properties.maxGenerations(), sessions::incrementGenerations,
        ErrorCodes.Anonymous.GENERATION_LIMIT_REACHED);
  }

  /** Devolve a geração consumida quando ela falhou antes de ser entregue; no-op para contas. */
  @Transactional
  public void refundGeneration(Owner owner) {
    if (isAnonymous(owner)) {
      sessions.decrementGenerations(owner.getId());
    }
  }

  private void consume(Owner owner, int max, Increment increment, String errorCode) {
    if (!isAnonymous(owner)) {
      return;
    }
    if (increment.apply(owner.getId(), max) == 0) {
      throw new TooManyRequestsException(errorCode);
    }
  }

  @FunctionalInterface
  private interface Increment {
    int apply(UUID ownerId, int max);
  }
}
