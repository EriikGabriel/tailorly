package com.tailorly.api.base;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseVersionRepository extends JpaRepository<BaseVersion, UUID> {
  Optional<BaseVersion> findFirstByDocumentIdOrderByVersionNumberDesc(UUID documentId);

  Optional<BaseVersion> findFirstByOwnerIdOrderByVersionNumberDesc(UUID ownerId);
}
