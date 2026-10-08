package com.tailorly.api.session;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousSessionRepository extends JpaRepository<AnonymousSession, UUID> {
  Optional<AnonymousSession> findByTokenHash(String tokenHash);

  boolean existsByOwnerId(UUID ownerId);
}
