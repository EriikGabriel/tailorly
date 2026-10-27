package com.tailorly.api.session;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.session.dto.AnonymousMigrationResponse;
import com.tailorly.api.session.dto.MigrateAnonymousSessionRequest;
import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceAlreadyExistsException;
import com.tailorly.api.shared.security.CurrentAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Migra para uma conta o snapshot revisado de uma sessão anônima, sem repetir OCR nem extração.
 *
 * <p>Move, para o owner da conta, a cadeia inteira que sustenta o snapshot ativo do visitante:
 * documento, versão, execuções de extração, páginas, fatos, evidências, pendências, decisões e o
 * snapshot. Os objetos do storage são copiados para o prefixo da conta; só depois os dados
 * restantes do visitante (outras versões candidatas, a sessão e o owner) são apagados. Nada é
 * reprocessado: um reenvio posterior do mesmo arquivo reaproveita a execução já concluída.</p>
 *
 * <p>Se a conta já tem um Ground Truth ativo, a migração só prossegue com
 * {@code replaceActive}: o ativo anterior vira {@code SUPERSEDED} e o migrado passa a ser o
 * ativo na mesma transação. Se a conta já tem o mesmo arquivo (mesmo hash), a versão migrada
 * passa a apontar para esse documento e a cópia do visitante é descartada.</p>
 */
@Service
public class AnonymousMigrationService {
  private static final Logger log = LoggerFactory.getLogger(AnonymousMigrationService.class);

  private final AnonymousSessionService sessions;
  private final CurrentAccountService currentAccount;
  private final AnonymousDataPurgeService purgeService;
  private final JdbcTemplate jdbc;
  private final ObjectStorage storage;
  private final TransactionTemplate transaction;

  public AnonymousMigrationService(AnonymousSessionService sessions,
      CurrentAccountService currentAccount, AnonymousDataPurgeService purgeService,
      JdbcTemplate jdbc, ObjectStorage storage, PlatformTransactionManager transactionManager) {
    this.sessions = sessions;
    this.currentAccount = currentAccount;
    this.purgeService = purgeService;
    this.jdbc = jdbc;
    this.storage = storage;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  public AnonymousMigrationResponse migrate(Authentication authentication,
      MigrateAnonymousSessionRequest request) {
    AnonymousPrincipal principal = sessions.authenticate(request.sessionToken())
        .orElseThrow(() -> new BadRequestException(ErrorCodes.Session.INVALID_TOKEN));
    UUID accountOwnerId = currentAccount.findOrCreate(authentication).owner().id();
    UUID anonymousOwnerId = principal.ownerId();

    AnonymousMigrationResponse response = transaction.execute(
        status -> moveChain(anonymousOwnerId, accountOwnerId, request.replaceActive()));

    // Só depois do commit: o que sobrou do visitante deixa de existir.
    purgeService.purgeOwner(anonymousOwnerId);
    return response;
  }

  private AnonymousMigrationResponse moveChain(UUID anonymousOwnerId, UUID accountOwnerId,
      boolean replaceActive) {
    // Trava os dois owners em ordem estável para não conflitar com uploads e aprovações em curso.
    jdbc.queryForList("select id from owners where id in (?, ?) order by id for update",
        anonymousOwnerId, accountOwnerId);

    UUID snapshotId = single("select active_snapshot_id from owners where id = ? "
        + "and kind = 'KIND_ANONYMOUS'", anonymousOwnerId);
    if (snapshotId == null) {
      throw new BadRequestException(ErrorCodes.Anonymous.NOTHING_TO_MIGRATE);
    }
    UUID previousActiveId = single("select active_snapshot_id from owners where id = ?",
        accountOwnerId);
    if (previousActiveId != null && !replaceActive) {
      throw new ResourceAlreadyExistsException(ErrorCodes.Anonymous.ACTIVE_SNAPSHOT_EXISTS);
    }

    UUID versionId = single("select base_version_id from gt_snapshots where id = ?", snapshotId);
    UUID documentId = single("select document_id from base_versions where id = ?", versionId);
    UUID existingDocumentId = single("select e.id from base_documents e "
        + "join base_documents d on d.content_hash = e.content_hash "
        + "where d.id = ? and e.owner_id = ?", documentId, accountOwnerId);
    boolean documentReused = existingDocumentId != null;

    List<MovedObject> objects = loadObjects(versionId, documentReused ? null : documentId);
    List<String> copiedKeys = copyObjects(objects, anonymousOwnerId, accountOwnerId);
    registerRollbackCleanup(copiedKeys);

    // O owner anônimo solta o snapshot antes de ele mudar de dono (FK composta id + owner).
    jdbc.update("update owners set active_snapshot_id = null where id = ?", anonymousOwnerId);

    if (documentReused) {
      jdbc.update("update base_versions set document_id = ? where id = ?",
          existingDocumentId, versionId);
    } else {
      jdbc.update("update base_documents set owner_id = ? where id = ?", accountOwnerId, documentId);
    }
    for (MovedObject object : objects) {
      jdbc.update("update stored_objects set owner_id = ?, storage_key = ?, expires_at = null "
          + "where id = ?", accountOwnerId, object.newKey(), object.id());
    }
    int versionNumber = nextNumber("select coalesce(max(version_number), 0) + 1 from base_versions "
        + "where owner_id = ?", accountOwnerId);
    jdbc.update("update base_versions set owner_id = ?, version_number = ? where id = ?",
        accountOwnerId, versionNumber, versionId);
    jdbc.update("update review_decisions set actor_owner_id = ? where base_version_id = ?",
        accountOwnerId, versionId);
    int snapshotNumber = nextNumber("select coalesce(max(snapshot_number), 0) + 1 from gt_snapshots "
        + "where owner_id = ?", accountOwnerId);
    jdbc.update("update gt_snapshots set owner_id = ?, snapshot_number = ? where id = ?",
        accountOwnerId, snapshotNumber, snapshotId);

    if (previousActiveId != null) {
      jdbc.update("update base_versions set status = 'SUPERSEDED' where id = "
          + "(select base_version_id from gt_snapshots where id = ?) and id <> ?",
          previousActiveId, versionId);
    }
    jdbc.update("update owners set active_snapshot_id = ?, revision = revision + 1 where id = ?",
        snapshotId, accountOwnerId);

    return AnonymousMigrationResponse.builder()
        .snapshotId(snapshotId)
        .baseVersionId(versionId)
        .documentId(documentReused ? existingDocumentId : documentId)
        .documentReused(documentReused)
        .replacedActive(previousActiveId != null)
        .build();
  }

  private List<MovedObject> loadObjects(UUID versionId, UUID originalDocumentId) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder(
        "select o.id, o.storage_key, o.mime_type from stored_objects o where o.id in (");
    if (originalDocumentId != null) {
      sql.append("select original_object_id from base_documents where id = ? union ");
      args.add(originalDocumentId);
    }
    sql.append("select output_object_id from extraction_runs where base_version_id = ? ")
        .append("and output_object_id is not null ")
        .append("union select p.text_object_id from extracted_pages p ")
        .append("join extraction_runs r on r.id = p.extraction_run_id where r.base_version_id = ?)");
    args.add(versionId);
    args.add(versionId);
    return jdbc.query(sql.toString(),
        (rs, row) -> new MovedObject(rs.getObject("id", UUID.class), rs.getString("storage_key"),
            rs.getString("mime_type"), null),
        args.toArray());
  }

  /** Copia cada objeto para o prefixo da conta e devolve as novas chaves gravadas. */
  private List<String> copyObjects(List<MovedObject> objects, UUID anonymousOwnerId,
      UUID accountOwnerId) {
    List<String> copied = new ArrayList<>();
    String anonymousPrefix = AnonymousDataPurgeService.prefix(anonymousOwnerId);
    String accountPrefix = AnonymousDataPurgeService.prefix(accountOwnerId);
    for (int index = 0; index < objects.size(); index++) {
      MovedObject object = objects.get(index);
      String newKey = object.oldKey().startsWith(anonymousPrefix)
          ? accountPrefix + object.oldKey().substring(anonymousPrefix.length())
          : accountPrefix + "migrated/" + UUID.randomUUID();
      storage.put(newKey, storage.get(object.oldKey()), object.mimeType());
      copied.add(newKey);
      objects.set(index, new MovedObject(object.id(), object.oldKey(), object.mimeType(), newKey));
    }
    return copied;
  }

  // Se a transação for desfeita, as cópias no prefixo da conta não têm registro e são removidas.
  private void registerRollbackCleanup(List<String> copiedKeys) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          copiedKeys.forEach(key -> {
            try {
              storage.delete(key);
            } catch (RuntimeException exception) {
              log.error("Could not remove rolled-back migration copy at {}", key, exception);
            }
          });
        }
      }
    });
  }

  private UUID single(String sql, Object... args) {
    List<UUID> rows = jdbc.queryForList(sql, UUID.class, args);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private int nextNumber(String sql, UUID ownerId) {
    Integer number = jdbc.queryForObject(sql, Integer.class, ownerId);
    return number == null ? 1 : number;
  }

  private record MovedObject(UUID id, String oldKey, String mimeType, String newKey) {
  }
}
