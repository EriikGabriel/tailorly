package com.tailorly.api.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  @Override
  @EntityGraph(attributePaths = "roles")
  Optional<User> findById(@NonNull UUID id);

  @EntityGraph(attributePaths = "roles")
  Optional<User> findByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.email = :email")
  Optional<User> findByEmailForUpdate(@Param("email") String email);

  boolean existsByEmail(String email);

  @Query("select u.id from User u where u.enabled = false and u.disabledAt <= :threshold "
      + "and u.dataPurgedAt is null")
  List<UUID> findIdsEligibleForRetentionPurge(@Param("threshold") Instant threshold, Pageable pageable);

  @Modifying
  @Query("update User u set u.dataPurgedAt = :purgedAt where u.id = :id")
  int markDataPurged(@Param("id") UUID id, @Param("purgedAt") Instant purgedAt);
}
