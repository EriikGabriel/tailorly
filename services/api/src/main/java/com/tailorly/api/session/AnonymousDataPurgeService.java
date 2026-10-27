package com.tailorly.api.session;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.objects.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exclusão completa dos dados de um owner anônimo: arquivo original, texto extraído, fatos,
 * evidências, pendências, decisões, snapshots, a sessão e o próprio owner.
 *
 * <p>Os objetos do storage são removidos antes das linhas do banco: se a remoção falhar, nada
 * foi apagado no banco e a limpeza tenta de novo no próximo ciclo, sem deixar bytes órfãos sem
 * registro. Depois do commit, uma varredura por prefixo remove o que um worker em andamento
 * possa ter gravado nesse intervalo. Só owners {@code KIND_ANONYMOUS} podem ser apagados por
 * aqui, para que um erro de chamada nunca apague uma conta.</p>
 */
@Service
public class AnonymousDataPurgeService {
  private static final Logger log = LoggerFactory.getLogger(AnonymousDataPurgeService.class);

  private final JdbcTemplate jdbc;
  private final ObjectStorage storage;
  private final TransactionTemplate transaction;

  public AnonymousDataPurgeService(JdbcTemplate jdbc, ObjectStorage storage,
      PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.storage = storage;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  public void purgeOwner(UUID ownerId) {
    assertAnonymous(ownerId);

    List<String> keys = jdbc.queryForList(
        "select storage_key from stored_objects where owner_id = ?", String.class, ownerId);
    keys.forEach(storage::delete);

    transaction.executeWithoutResult(status -> deleteRows(ownerId));

    try {
      storage.deleteByPrefix(prefix(ownerId));
    } catch (RuntimeException exception) {
      log.warn("Could not sweep storage prefix of purged anonymous owner {}", ownerId, exception);
    }
  }

  public static String prefix(UUID ownerId) {
    return "owners/" + ownerId + "/";
  }

  private void assertAnonymous(UUID ownerId) {
    List<String> kinds = jdbc.queryForList(
        "select kind from owners where id = ?", String.class, ownerId);
    if (kinds.isEmpty()) {
      return;
    }
    if (!"KIND_ANONYMOUS".equals(kinds.getFirst())) {
      throw new IllegalStateException("Refusing to purge a non-anonymous owner");
    }
  }

  private void deleteRows(UUID ownerId) {
    String versions = "(select id from base_versions where owner_id = ?)";
    String runs = "(select id from extraction_runs where base_version_id in " + versions + ")";
    String facts = "(select f.id from facts f join fact_records r on r.id = f.record_id "
        + "where r.base_version_id in " + versions + ")";

    jdbc.update("delete from gt_snapshot_items where snapshot_id in "
        + "(select id from gt_snapshots where owner_id = ?)", ownerId);
    jdbc.update("update owners set active_snapshot_id = null where id = ?", ownerId);
    jdbc.update("delete from gt_snapshots where owner_id = ?", ownerId);
    jdbc.update("delete from review_decisions where actor_owner_id = ? or base_version_id in "
        + versions, ownerId, ownerId);
    jdbc.update("delete from review_issues where base_version_id in " + versions, ownerId);
    jdbc.update("delete from fact_evidence where fact_id in " + facts, ownerId);
    jdbc.update("delete from facts where id in " + facts, ownerId);
    jdbc.update("delete from fact_records where base_version_id in " + versions, ownerId);
    jdbc.update("delete from extracted_pages where extraction_run_id in " + runs, ownerId);
    jdbc.update("delete from extraction_runs where base_version_id in " + versions, ownerId);
    jdbc.update("delete from base_versions where owner_id = ?", ownerId);
    jdbc.update("delete from base_documents where owner_id = ?", ownerId);
    jdbc.update("delete from stored_objects where owner_id = ?", ownerId);
    jdbc.update("delete from ai_preferences where owner_id = ?", ownerId);
    jdbc.update("delete from anonymous_sessions where owner_id = ?", ownerId);
    jdbc.update("delete from owners where id = ? and kind = 'KIND_ANONYMOUS'", ownerId);
  }
}
