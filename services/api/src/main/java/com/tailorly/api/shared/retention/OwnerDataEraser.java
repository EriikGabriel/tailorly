package com.tailorly.api.shared.retention;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.objects.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Exclusão em cascata dos dados pessoais de um owner: currículo base (arquivo, texto extraído,
 * fatos, evidências, pendências, decisões), snapshots do Ground Truth, vagas lidas e currículos
 * gerados. Usada tanto pela purga completa de owners anônimos quanto pela retenção de contas.
 *
 * <p>Não apaga {@code owners}, {@code anonymous_sessions} nem {@code users}: quem chama decide
 * se a própria identidade do owner também deve sumir (sessão anônima purgada) ou se só os dados
 * pessoais saem, mantendo a conta desativada (retenção de conta).</p>
 *
 * <p>Os objetos do storage são removidos antes das linhas do banco: se a remoção falhar, nada
 * foi apagado no banco e a limpeza pode ser repetida sem deixar bytes órfãos sem registro.</p>
 */
@Component
public class OwnerDataEraser {
  private static final Logger log = LoggerFactory.getLogger(OwnerDataEraser.class);

  private final JdbcTemplate jdbc;
  private final ObjectStorage storage;

  public OwnerDataEraser(JdbcTemplate jdbc, ObjectStorage storage) {
    this.jdbc = jdbc;
    this.storage = storage;
  }

  /** Apaga do storage todo objeto hoje registrado em {@code stored_objects} para o owner. */
  public void deleteFiles(UUID ownerId) {
    List<String> keys = jdbc.queryForList(
        "select storage_key from stored_objects where owner_id = ?", String.class, ownerId);
    keys.forEach(storage::delete);
  }

  /**
   * Apaga as linhas de banco ligadas aos dados pessoais do owner, em ordem compatível com as FKs.
   * As tabelas de evidência complementar caem em cascata com o snapshot, mas também são apagadas
   * aqui pelo owner, para não depender só dessa cascata. Deve rodar dentro da transação do chamador,
   * depois de {@link #deleteFiles}.
   */
  public void eraseOwnerData(UUID ownerId) {
    String versions = "(select id from base_versions where owner_id = ?)";
    String runs = "(select id from extraction_runs where base_version_id in " + versions + ")";
    String facts = "(select f.id from facts f join fact_records r on r.id = f.record_id "
        + "where r.base_version_id in " + versions + ")";

    // Currículos gerados contêm dados pessoais e saem antes dos snapshots e da vaga que citam.
    jdbc.update("delete from cv_generations where owner_id = ?", ownerId);
    jdbc.update("delete from cv_templates where owner_id = ?", ownerId);
    jdbc.update("delete from cv_supplementary_evidence where owner_id = ?", ownerId);
    jdbc.update("delete from cv_evidence_link_checks where owner_id = ?", ownerId);
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
    jdbc.update("delete from job_posting_versions where job_posting_id in "
        + "(select id from job_postings where owner_id = ?)", ownerId);
    jdbc.update("delete from job_postings where owner_id = ?", ownerId);
    jdbc.update("delete from ai_preferences where owner_id = ?", ownerId);
    jdbc.update("delete from owner_vaults where owner_id = ?", ownerId);
    jdbc.update("delete from owner_generation_defaults where owner_id = ?", ownerId);
  }

  /** Varredura por prefixo depois do commit, para o que um worker em andamento tenha gravado. */
  public void sweepStoragePrefix(String prefix) {
    try {
      storage.deleteByPrefix(prefix);
    } catch (RuntimeException exception) {
      log.warn("Could not sweep storage prefix {}", prefix, exception);
    }
  }

  public static String prefix(UUID ownerId) {
    return "owners/" + ownerId + "/";
  }
}
