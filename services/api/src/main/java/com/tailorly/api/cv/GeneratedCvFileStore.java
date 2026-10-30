package com.tailorly.api.cv;

import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.objects.StoredObjectRepository;
import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.session.AnonymousQuotaService;
import com.tailorly.api.shared.util.Sha256;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Grava os arquivos gerados (PDF e Markdown) no storage privado do owner e registra o objeto. */
@Component
@RequiredArgsConstructor
class GeneratedCvFileStore {
  private static final Logger log = LoggerFactory.getLogger(GeneratedCvFileStore.class);

  private final ObjectStorage storage;
  private final StoredObjectRepository objects;

  /** Deve ser chamado dentro de uma transação: se ela for desfeita, o arquivo gravado é removido. */
  StoredObject store(Owner owner, String purpose, String key, byte[] content, String mimeType) {
    storage.put(key, content, mimeType);
    // Se a transação for desfeita depois da gravação, o objeto não teria registro algum.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          try {
            storage.delete(key);
          } catch (RuntimeException exception) {
            log.error("Could not remove rolled-back generated file at {}", key, exception);
          }
        }
      }
    });
    return objects.save(StoredObject.builder()
        .owner(owner)
        .purpose(purpose)
        .storageKey(key)
        .contentHash(Sha256.hex(content))
        .mimeType(mimeType)
        .sizeBytes((long) content.length)
        // Dados de visitantes herdam o TTL da sessão; contas não expiram.
        .expiresAt(AnonymousQuotaService.isAnonymous(owner) ? owner.getExpiresAt() : null)
        .deletionStatus("ACTIVE")
        .build());
  }
}
