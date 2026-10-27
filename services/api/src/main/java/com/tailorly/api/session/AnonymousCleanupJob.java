package com.tailorly.api.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Apaga periodicamente os dados de sessões anônimas expiradas. Sem migração para uma conta
 * antes da expiração, nada do visitante permanece.
 */
public class AnonymousCleanupJob {
  private static final Logger log = LoggerFactory.getLogger(AnonymousCleanupJob.class);
  private static final int BATCH_SIZE = 50;

  private final AnonymousSessionRepository sessions;
  private final AnonymousDataPurgeService purge;

  public AnonymousCleanupJob(AnonymousSessionRepository sessions, AnonymousDataPurgeService purge) {
    this.sessions = sessions;
    this.purge = purge;
  }

  @Scheduled(fixedDelayString = "${tailorly.anonymous.cleanup-interval:PT5M}",
      initialDelayString = "${tailorly.anonymous.cleanup-interval:PT5M}")
  public void run() {
    purgeExpired(Instant.now());
  }

  /** Retorna quantos owners foram apagados; falhas individuais não interrompem o lote. */
  public int purgeExpired(Instant now) {
    int purged = 0;
    List<UUID> ownerIds = sessions.findExpiredOwnerIds(now, PageRequest.of(0, BATCH_SIZE));
    for (UUID ownerId : ownerIds) {
      try {
        purge.purgeOwner(ownerId);
        purged++;
      } catch (RuntimeException exception) {
        log.warn("Could not purge expired anonymous owner {}; will retry", ownerId, exception);
      }
    }
    if (purged > 0) {
      log.info("Purged {} expired anonymous sessions", purged);
    }
    return purged;
  }
}
