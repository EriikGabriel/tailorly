package com.tailorly.api.owner;

import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {
  Optional<Owner> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Owner o where o.id = :id")
  Optional<Owner> findByIdForUpdate(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Owner o set o.revision = o.revision + 1 where o.id = :id")
  int incrementRevision(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Owner o set o.activeSnapshotId = :snapshotId where o.id = :id")
  int updateActiveSnapshot(
      @Param("id") UUID id,
      @Param("snapshotId") UUID snapshotId
  );
}
