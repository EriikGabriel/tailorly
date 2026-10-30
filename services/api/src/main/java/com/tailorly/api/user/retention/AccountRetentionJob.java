package com.tailorly.api.user.retention;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tailorly.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Apaga periodicamente os dados pessoais de contas desativadas há mais tempo que o período de
 * retenção configurado. Espelha {@code com.tailorly.api.session.AnonymousCleanupJob}, mas a
 * conta em si (linha de {@code users}/{@code owners}) permanece — só os dados do owner somem.
 */
public class AccountRetentionJob {
  private static final Logger log = LoggerFactory.getLogger(AccountRetentionJob.class);
  private static final int BATCH_SIZE = 50;

  private final UserRepository users;
  private final AccountDataRetentionService retention;
  private final AccountRetentionProperties properties;

  public AccountRetentionJob(UserRepository users, AccountDataRetentionService retention,
      AccountRetentionProperties properties) {
    this.users = users;
    this.retention = retention;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${tailorly.account.cleanup-interval:P1D}",
      initialDelayString = "${tailorly.account.cleanup-interval:P1D}")
  public void run() {
    purgeEligible(Instant.now());
  }

  /** Retorna quantas contas foram purgadas; falhas individuais não interrompem o lote. */
  public int purgeEligible(Instant now) {
    Instant threshold = now.minus(properties.retentionPeriod());
    int purged = 0;
    List<UUID> userIds = users.findIdsEligibleForRetentionPurge(threshold, PageRequest.of(0, BATCH_SIZE));
    for (UUID userId : userIds) {
      try {
        retention.purgeAccount(userId);
        purged++;
      } catch (RuntimeException exception) {
        log.warn("Could not purge retained data for account {}; will retry", userId, exception);
      }
    }
    if (purged > 0) {
      log.info("Purged personal data of {} accounts past the retention period", purged);
    }
    return purged;
  }
}
