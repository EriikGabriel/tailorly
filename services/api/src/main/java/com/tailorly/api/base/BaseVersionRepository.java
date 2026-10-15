package com.tailorly.api.base;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseVersionRepository extends JpaRepository<BaseVersion, UUID> {
  boolean existsByOwnerIdAndVersionNumber(UUID ownerId, int versionNumber);
}
