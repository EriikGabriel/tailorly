package com.tailorly.api.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousSessionRepository extends JpaRepository<AnonymousSession, UUID> {
  Optional<AnonymousSession> findByTokenHash(String tokenHash);

  Optional<AnonymousSession> findByOwnerId(UUID ownerId);

  boolean existsByOwnerId(UUID ownerId);

  long countByExpiresAtAfterAndRevokedAtIsNull(Instant now);

  @Query("select s.owner.id from AnonymousSession s where s.expiresAt <= :now order by s.expiresAt")
  List<UUID> findExpiredOwnerIds(@Param("now") Instant now, Pageable pageable);

  // Os incrementos são atômicos e só acontecem abaixo do limite, então requisições
  // concorrentes não conseguem ultrapassá-lo. Sem clearAutomatically para não desanexar as
  // entidades da transação do chamador.
  @Modifying(flushAutomatically = true)
  @Query("update AnonymousSession s set s.uploadsCount = s.uploadsCount + 1 "
      + "where s.owner.id = :ownerId and s.uploadsCount < :max")
  int incrementUploads(@Param("ownerId") UUID ownerId, @Param("max") int max);

  @Modifying(flushAutomatically = true)
  @Query("update AnonymousSession s set s.extractionsCount = s.extractionsCount + 1 "
      + "where s.owner.id = :ownerId and s.extractionsCount < :max")
  int incrementExtractions(@Param("ownerId") UUID ownerId, @Param("max") int max);

  @Modifying(flushAutomatically = true)
  @Query("update AnonymousSession s set s.generationsCount = s.generationsCount + 1 "
      + "where s.owner.id = :ownerId and s.generationsCount < :max")
  int incrementGenerations(@Param("ownerId") UUID ownerId, @Param("max") int max);

  /** Devolve uma geração que não chegou a ser entregue (falha depois de consumida). */
  @Modifying(flushAutomatically = true)
  @Query("update AnonymousSession s set s.generationsCount = s.generationsCount - 1 "
      + "where s.owner.id = :ownerId and s.generationsCount > 0")
  int decrementGenerations(@Param("ownerId") UUID ownerId);
}
