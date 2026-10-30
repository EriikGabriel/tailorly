package com.tailorly.api.user.retention;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.owner.OwnerRepository;
import com.tailorly.api.shared.retention.OwnerDataEraser;
import com.tailorly.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aplica a retenção a uma conta desativada: apaga os dados pessoais do owner da conta (mesma
 * exclusão em cascata usada para owners anônimos, via {@link OwnerDataEraser}) e marca
 * {@code users.data_purged_at}. Não apaga {@code users} nem {@code owners} — a conta continua
 * existindo, desativada, só sem os dados pessoais.
 */
@Service
public class AccountDataRetentionService {
  private final UserRepository users;
  private final OwnerRepository owners;
  private final OwnerDataEraser eraser;
  private final TransactionTemplate transaction;

  public AccountDataRetentionService(UserRepository users, OwnerRepository owners,
      OwnerDataEraser eraser, PlatformTransactionManager transactionManager) {
    this.users = users;
    this.owners = owners;
    this.eraser = eraser;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  public void purgeAccount(UUID userId) {
    UUID ownerId = owners.findByUserId(userId).map(owner -> owner.getId()).orElse(null);

    if (ownerId != null) {
      eraser.deleteFiles(ownerId);
    }

    transaction.executeWithoutResult(status -> {
      if (ownerId != null) {
        eraser.eraseOwnerData(ownerId);
      }
      users.markDataPurged(userId, Instant.now());
    });

    if (ownerId != null) {
      eraser.sweepStoragePrefix(OwnerDataEraser.prefix(ownerId));
    }
  }
}
