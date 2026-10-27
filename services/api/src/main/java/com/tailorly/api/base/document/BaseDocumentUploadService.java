package com.tailorly.api.base.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import com.tailorly.api.base.document.dto.BaseDocumentResponse;
import com.tailorly.api.base.document.dto.BaseDocumentUploadResponse;
import com.tailorly.api.base.version.BaseVersionService;
import com.tailorly.api.base.version.dto.BaseVersionResponse;
import com.tailorly.api.extraction.ExtractionQueue;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recebe o upload multipart do Currículo Base, valida o PDF, deduplica por hash dentro do
 * mesmo owner e dispara a criação/reaproveitamento da versão candidata e da fila de extração.
 *
 * <p>Fluxo de {@link #upload}, tudo em uma única transação com o owner bloqueado
 * ({@link com.tailorly.api.owner.OwnerRepository#findByIdForUpdate}) para serializar uploads
 * concorrentes da mesma conta:</p>
 * <ol>
 *   <li>Valida tamanho, formato e assinatura do PDF.</li>
 *   <li>Calcula o SHA-256 dos bytes e procura um {@link BaseDocument} com o mesmo hash
 *       <strong>no mesmo owner</strong> (nunca entre owners diferentes).</li>
 *   <li>Se já existir, confirma que o arquivo original ainda existe no storage e reaproveita
 *       o documento; caso contrário, grava os bytes no bucket privado e registra
 *       {@code StoredObject}/{@code BaseDocument} novos.</li>
 *   <li>Delega a {@link BaseVersionService#findOrCreateForUpload} a criação da versão
 *       {@code CANDIDATE} (ou o reaproveitamento de uma versão existente do mesmo documento) e
 *       enfileira a extração correspondente.</li>
 * </ol>
 *
 * <p>Se a transação for desfeita depois que o arquivo já foi gravado no storage, um
 * {@link org.springframework.transaction.support.TransactionSynchronization} registrado em
 * {@link #upload} remove o objeto órfão do storage para evitar bytes sem metadados.</p>
 */
@Service
@RequiredArgsConstructor
public class BaseDocumentUploadService {
  private static final Logger log = LoggerFactory.getLogger(BaseDocumentUploadService.class);
  private static final long MAX_PDF_SIZE = 7L * 1024 * 1024;
  private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

  private final OwnerRepository ownerRepository;
  private final OwnerAccessService ownerAccessService;
  private final BaseDocumentRepository documentRepository;
  private final BaseVersionService versionService;
  private final ExtractionQueue extractionQueue;
  private final StoredObjectRepository objectRepository;
  private final ObjectStorage storage;
  private final AnonymousQuotaService anonymousQuota;

  @Transactional
  public BaseDocumentUploadResponse upload(UUID ownerId, MultipartFile file) {
    Owner owner = ownerRepository.findByIdForUpdate(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
    ownerAccessService.assertCanAccess(owner);

    boolean anonymous = AnonymousQuotaService.isAnonymous(owner);

    if (file == null || file.isEmpty()) {
      throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
    }

    if (file.getSize() > MAX_PDF_SIZE) {
      throw new BadRequestException(ErrorCodes.BaseDocument.FILE_TOO_LARGE);
    }

    if (anonymous) {
      anonymousQuota.assertFileSize(file.getSize());
    }

    byte[] bytes = readPdf(file);
    String hash = sha256(bytes);

    // Deduplicação é sempre escopada ao owner: o mesmo PDF enviado por outra pessoa
    // cria um BaseDocument independente.
    var existing = documentRepository.findByOwnerIdAndContentHash(ownerId, hash);
    if (existing.isPresent()) {
      if (!storage.exists(existing.get().getOriginalObject().getStorageKey())) {
        throw new ResourceAlreadyExistsException(ErrorCodes.BaseDocument.ORIGINAL_OBJECT_MISSING);
      }
      return response(existing.get(), true);
    }

    // Só um arquivo novo consome o limite de uploads anônimos; reenvio idêntico reaproveita o existente.
    Integer pageCount = null;
    if (anonymous) {
      pageCount = anonymousQuota.assertPageLimit(bytes);
      anonymousQuota.consumeUpload(owner);
    }

    String key = "owners/" + ownerId + "/originals/" + UUID.randomUUID() + ".pdf";
    storage.put(key, bytes, "application/pdf");

    // Compensação: se a transação do banco for desfeita após a gravação no storage,
    // o objeto órfão precisa ser removido para não acumular bytes sem metadados.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          try {
            storage.delete(key);
          } catch (RuntimeException ex) {
            log.error("Could not remove rolled-back upload at {}", key, ex);
          }
        }
      }
    });

    StoredObject object = StoredObject.builder()
        .owner(owner)
        .purpose("BASE_ORIGINAL")
        .storageKey(key)
        .contentHash(hash)
        .mimeType("application/pdf")
        .sizeBytes((long) bytes.length)
        // Dados de visitantes herdam o TTL da sessão; contas não expiram.
        .expiresAt(anonymous ? owner.getExpiresAt() : null)
        .deletionStatus("ACTIVE")
        .build();
    objectRepository.save(object);

    BaseDocument document = BaseDocument.builder()
        .owner(owner)
        .originalObject(object)
        .contentHash(hash)
        .originalFilename(safeFilename(file.getOriginalFilename()))
        .pageCount(pageCount)
        .build();
    documentRepository.saveAndFlush(document);

    return response(document, false);
  }

  private BaseDocumentUploadResponse response(BaseDocument document, boolean reused) {
    var selection = versionService.findOrCreateForUpload(document);
    extractionQueue.enqueue(selection.version());
    return BaseDocumentUploadResponse.builder()
        .document(BaseDocumentResponse.from(document))
        .reused(reused)
        .version(BaseVersionResponse.from(selection.version()))
        .versionReused(selection.reused())
        .build();
  }

  private byte[] readPdf(MultipartFile file) {
    try {
      byte[] bytes = file.getBytes();
      if (bytes.length == 0 || bytes.length > MAX_PDF_SIZE) {
        throw new BadRequestException(bytes.length == 0
            ? ErrorCodes.BaseDocument.INVALID_PDF : ErrorCodes.BaseDocument.FILE_TOO_LARGE);
      }
      if (bytes.length < PDF_HEADER.length) {
        throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
      }
      for (int i = 0; i < PDF_HEADER.length; i++) {
        if (bytes[i] != PDF_HEADER[i]) {
          throw new BadRequestException(ErrorCodes.BaseDocument.INVALID_PDF);
        }
      }
      return bytes;
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read uploaded PDF", ex);
    }
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private String safeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return "curriculo.pdf";
    }
    String basename = filename.replace('\\', '/');
    basename = basename.substring(basename.lastIndexOf('/') + 1);
    return basename.isBlank() ? "curriculo.pdf" : basename.substring(0, Math.min(255, basename.length()));
  }
}
