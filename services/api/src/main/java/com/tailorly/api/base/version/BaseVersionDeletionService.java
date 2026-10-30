package com.tailorly.api.base.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tailorly.api.objects.storage.ObjectStorage;
import com.tailorly.api.owner.Owner;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exclusão efetiva de uma {@link BaseVersion}: arquivo original (se nenhuma outra versão do
 * mesmo documento sobrar), texto extraído, fatos, evidências, pendências, decisões, os
 * snapshots do Ground Truth aprovados a partir dela e os currículos gerados a partir desses
 * snapshots — eles contêm os mesmos dados pessoais e a invariante do domínio exige que saiam
 * junto. Se a versão era o Ground Truth ativo do owner, a exclusão também desativa (zera
 * {@code owners.active_snapshot_id}); diferente da desativação isolada
 * ({@code OwnerLifecycleService}), aqui não há dado para manter.
 *
 * <p>Nenhuma FK do schema tem {@code ON DELETE CASCADE}, então a ordem das instruções é
 * significativa, no mesmo espírito de {@code com.tailorly.api.shared.retention.OwnerDataEraser}.
 * Os objetos de storage são identificados e apagados <b>antes</b> das linhas que os referenciam
 * saírem do banco (senão as junções que os localizam deixariam de encontrar nada); se a remoção
 * no storage falhar, nada foi apagado no banco e a exclusão pode ser repetida.</p>
 */
@Service
public class BaseVersionDeletionService {
  private final BaseVersionRepository versions;
  private final OwnerRepository owners;
  private final OwnerAccessService access;
  private final JdbcTemplate jdbc;
  private final ObjectStorage storage;
  private final TransactionTemplate transaction;

  public BaseVersionDeletionService(BaseVersionRepository versions, OwnerRepository owners,
      OwnerAccessService access, JdbcTemplate jdbc, ObjectStorage storage,
      PlatformTransactionManager transactionManager) {
    this.versions = versions;
    this.owners = owners;
    this.access = access;
    this.jdbc = jdbc;
    this.storage = storage;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  public void delete(UUID baseVersionId) {
    // Tudo numa única transação: owners.findByIdForUpdate usa PESSIMISTIC_WRITE, que o JPA exige
    // rodar dentro de uma transação de escrita em andamento; separar a trava da exclusão também
    // deixaria o lock sem efeito, pois ele seria liberado antes da exclusão acontecer.
    transaction.executeWithoutResult(status -> {
      BaseVersion version = versions.findById(baseVersionId)
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
      UUID ownerId = version.getOwner().getId();
      UUID documentId = version.getDocument().getId();

      Owner owner = owners.findByIdForUpdate(ownerId)
          .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.Owner.NOT_FOUND));
      access.assertCanAccess(owner);

      boolean documentOrphaned = documentHasNoOtherVersion(baseVersionId, documentId);
      List<StoredObjectRef> refs = collectStoredObjectRefs(baseVersionId, documentId, documentOrphaned);
      refs.forEach(ref -> storage.delete(ref.storageKey()));

      deleteRows(baseVersionId, documentId, documentOrphaned, refs);
    });
  }

  private record StoredObjectRef(UUID id, String storageKey) {
  }

  private List<StoredObjectRef> collectStoredObjectRefs(UUID baseVersionId, UUID documentId,
      boolean documentOrphaned) {
    List<StoredObjectRef> refs = new ArrayList<>();
    refs.addAll(query("select so.id, so.storage_key from stored_objects so "
        + "join cv_generations g on g.pdf_object_id = so.id "
        + "join gt_snapshots s on s.id = g.snapshot_id where s.base_version_id = ?", baseVersionId));
    refs.addAll(query("select so.id, so.storage_key from stored_objects so "
        + "join cv_generations g on g.markdown_object_id = so.id "
        + "join gt_snapshots s on s.id = g.snapshot_id where s.base_version_id = ?", baseVersionId));
    refs.addAll(query("select so.id, so.storage_key from stored_objects so "
        + "join extraction_runs r on r.output_object_id = so.id where r.base_version_id = ?",
        baseVersionId));
    refs.addAll(query("select so.id, so.storage_key from stored_objects so "
        + "join extracted_pages p on p.text_object_id = so.id "
        + "join extraction_runs r on r.id = p.extraction_run_id where r.base_version_id = ?",
        baseVersionId));
    if (documentOrphaned) {
      refs.addAll(query("select so.id, so.storage_key from stored_objects so "
          + "join base_documents d on d.original_object_id = so.id where d.id = ?", documentId));
    }
    return refs;
  }

  private List<StoredObjectRef> query(String sql, UUID param) {
    return jdbc.queryForList(sql, param).stream()
        .map(this::toRef)
        .collect(Collectors.toList());
  }

  private StoredObjectRef toRef(Map<String, Object> row) {
    return new StoredObjectRef((UUID) row.get("id"), (String) row.get("storage_key"));
  }

  private boolean documentHasNoOtherVersion(UUID baseVersionId, UUID documentId) {
    Long otherVersions = jdbc.queryForObject(
        "select count(*) from base_versions where document_id = ? and id <> ?",
        Long.class, documentId, baseVersionId);
    return otherVersions == null || otherVersions == 0;
  }

  private void deleteRows(UUID baseVersionId, UUID documentId, boolean documentOrphaned,
      List<StoredObjectRef> refs) {
    String snapshots = "(select id from gt_snapshots where base_version_id = ?)";
    String runs = "(select id from extraction_runs where base_version_id = ?)";
    String facts = "(select f.id from facts f join fact_records r on r.id = f.record_id "
        + "where r.base_version_id = ?)";

    jdbc.update("delete from cv_generations where snapshot_id in " + snapshots, baseVersionId);
    jdbc.update("delete from gt_snapshot_items where snapshot_id in " + snapshots, baseVersionId);
    jdbc.update("update owners set active_snapshot_id = null where active_snapshot_id in "
        + snapshots, baseVersionId);
    jdbc.update("delete from gt_snapshots where base_version_id = ?", baseVersionId);
    jdbc.update("delete from review_decisions where base_version_id = ?", baseVersionId);
    jdbc.update("delete from review_issues where base_version_id = ?", baseVersionId);
    jdbc.update("delete from fact_evidence where fact_id in " + facts, baseVersionId);
    jdbc.update("delete from facts where id in " + facts, baseVersionId);
    jdbc.update("delete from fact_records where base_version_id = ?", baseVersionId);
    jdbc.update("delete from extracted_pages where extraction_run_id in " + runs, baseVersionId);
    jdbc.update("delete from extraction_runs where base_version_id = ?", baseVersionId);
    jdbc.update("delete from base_versions where id = ?", baseVersionId);
    if (documentOrphaned) {
      jdbc.update("delete from base_documents where id = ?", documentId);
    }
    for (StoredObjectRef ref : refs) {
      jdbc.update("delete from stored_objects where id = ?", ref.id());
    }
  }
}
