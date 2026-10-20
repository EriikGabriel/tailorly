package com.tailorly.api.base.document;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseDocumentRepository extends JpaRepository<BaseDocument, UUID> {
  boolean existsByOwnerIdAndContentHash(UUID ownerId, String contentHash);

  Optional<BaseDocument> findByOwnerIdAndContentHash(UUID ownerId, String contentHash);

  List<BaseDocument> findAllByOwnerId(UUID ownerId);

  List<BaseDocument> findAllByContentHash(String contentHash);
}
