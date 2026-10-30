package com.tailorly.api.base.version;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseVersionRepository extends JpaRepository<BaseVersion, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from BaseVersion v where v.id = :id")
  Optional<BaseVersion> findByIdForUpdate(@Param("id") UUID id);

  Optional<BaseVersion> findFirstByDocumentIdOrderByVersionNumberDesc(UUID documentId);

  Optional<BaseVersion> findFirstByOwnerIdOrderByVersionNumberDesc(UUID ownerId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update BaseVersion v set v.status = :newStatus "
      + "where v.owner.id = :ownerId and v.status = :oldStatus")
  int updateStatusByOwnerAndStatus(
      @Param("ownerId") UUID ownerId,
      @Param("oldStatus") String oldStatus,
      @Param("newStatus") String newStatus
  );
}
