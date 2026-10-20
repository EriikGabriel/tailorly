package com.tailorly.api.base.version;

import java.util.UUID;

import com.tailorly.api.base.document.BaseDocument;
import com.tailorly.api.base.version.dto.BaseVersionResponse;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria ou reaproveita a versão candidata de um {@link BaseDocument} enviado.
 *
 * <p>Não altera o snapshot ativo do owner: a candidata só se torna o Ground Truth depois de
 * extração e revisão (fluxo ainda planejado). Este serviço cuida apenas da numeração e do
 * reaproveitamento de versões; o enfileiramento da extração fica a cargo de quem chama
 * {@link #findOrCreateForUpload} (veja
 * {@code com.tailorly.api.base.document.BaseDocumentUploadService}).</p>
 */
@Service
@RequiredArgsConstructor
public class BaseVersionService {
  public static final String CANDIDATE = "CANDIDATE";

  private final BaseVersionRepository baseVersionRepository;
  private final OwnerAccessService ownerAccessService;

  /**
   * Seleciona a versão do upload atual.
   *
   * <p>Se o mesmo {@code document} já tiver uma versão (reenvio idêntico do mesmo arquivo),
   * reaproveita a de maior número sem criar outra e apenas marca
   * {@code temporalReviewRequired}, pois fatos sensíveis ao tempo podem precisar de nova
   * confirmação mesmo sem OCR novo. Caso contrário, cria uma candidata com o próximo número
   * de versão do owner.</p>
   */
  public VersionSelection findOrCreateForUpload(BaseDocument document) {
    var existing = baseVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId());
    if (existing.isPresent()) {
      BaseVersion version = existing.get();

      version.setTemporalReviewRequired(true);
      return new VersionSelection(baseVersionRepository.saveAndFlush(version), true);
    }

    int nextNumber = baseVersionRepository
        .findFirstByOwnerIdOrderByVersionNumberDesc(document.getOwner().getId())
        .map(version -> Math.addExact(version.getVersionNumber(), 1))
        .orElse(1);
    BaseVersion candidate = BaseVersion.builder()
        .owner(document.getOwner())
        .document(document)
        .versionNumber(nextNumber)
        .status(CANDIDATE)
        .reviewRevision(0)
        .temporalReviewRequired(true)
        .build();
    return new VersionSelection(baseVersionRepository.saveAndFlush(candidate), false);
  }

  @Transactional(readOnly = true)
  public BaseVersionResponse findById(UUID id) {
    BaseVersion version = getVersion(id);
    ownerAccessService.assertCanAccess(version.getOwner());

    return BaseVersionResponse.from(version);
  }

  private BaseVersion getVersion(UUID id) {
    return baseVersionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }

  public record VersionSelection(BaseVersion version, boolean reused) {
  }
}
