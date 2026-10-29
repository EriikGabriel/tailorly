package com.tailorly.api.cv.template;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvTemplateRepository extends JpaRepository<CvTemplate, UUID> {
  Optional<CvTemplate> findByNameAndVersionAndOwnerIsNull(String name, int version);

  Optional<CvTemplate> findFirstByOwnerIdAndNameOrderByVersionDesc(UUID ownerId, String name);

  long countByOwnerId(UUID ownerId);
}
