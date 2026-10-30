package com.tailorly.api.shared.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  Optional<UserSession> findByTokenHash(String tokenHash);

  List<UserSession> findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query("update UserSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
  void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
