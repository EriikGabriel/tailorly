package com.tailorly.api.session;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.shared.retention.OwnerDataEraser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exclusão completa dos dados de um owner anônimo: arquivo original, texto extraído, fatos,
 * evidências, pendências, decisões, snapshots, vagas lidas (com versões e análises), a sessão e o
 * próprio owner.
 *
 * <p>A exclusão dos dados pessoais em si (currículo base, snapshots, gerações, vagas) é
 * compartilhada com a retenção de contas via {@link OwnerDataEraser}; aqui só se adiciona a
 * remoção da própria sessão e do owner, que a retenção de conta não faz. Só owners
 * {@code KIND_ANONYMOUS} podem ser apagados por aqui, para que um erro de chamada nunca apague
 * uma conta.</p>
 */
@Service
public class AnonymousDataPurgeService {
  private final JdbcTemplate jdbc;
  private final OwnerDataEraser eraser;
  private final TransactionTemplate transaction;

  public AnonymousDataPurgeService(JdbcTemplate jdbc, OwnerDataEraser eraser,
      PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.eraser = eraser;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  public void purgeOwner(UUID ownerId) {
    assertAnonymous(ownerId);

    eraser.deleteFiles(ownerId);

    transaction.executeWithoutResult(status -> {
      eraser.eraseOwnerData(ownerId);
      jdbc.update("delete from anonymous_sessions where owner_id = ?", ownerId);
      jdbc.update("delete from owners where id = ? and kind = 'KIND_ANONYMOUS'", ownerId);
    });

    eraser.sweepStoragePrefix(OwnerDataEraser.prefix(ownerId));
  }

  public static String prefix(UUID ownerId) {
    return OwnerDataEraser.prefix(ownerId);
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
}
