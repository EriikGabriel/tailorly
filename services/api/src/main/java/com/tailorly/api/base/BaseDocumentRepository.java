package com.tailorly.api.base;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseDocumentRepository extends JpaRepository<BaseDocument, UUID> {
  boolean existsByOwnerIdAndContentHash(UUID ownerId, String contentHash);

  List<BaseDocument> findAllByOwnerId(UUID ownerId);

  List<BaseDocument> findAllByContentHash(String contentHash);
}
