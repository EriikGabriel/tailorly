package com.tailorly.api.base.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtSnapshotRepository extends JpaRepository<GtSnapshot, UUID> {
  List<GtSnapshot> findAllByOwnerIdOrderBySnapshotNumberDesc(UUID ownerId);

  Optional<GtSnapshot> findFirstByOwnerIdOrderBySnapshotNumberDesc(UUID ownerId);

  Optional<GtSnapshot> findByIdAndOwnerId(UUID id, UUID ownerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from GtSnapshot s where s.id = :id and s.owner.id = :ownerId")
  Optional<GtSnapshot> findByIdAndOwnerIdForUpdate(
      @Param("id") UUID id,
      @Param("ownerId") UUID ownerId
  );
}
