package com.tailorly.api.aipreferences;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPreferencesRepository extends JpaRepository<AiPreferences, UUID> {
  Optional<AiPreferences> findByOwnerId(UUID ownerId);
}
