package com.tailorly.api.base.fact;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.Fact;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactRepository extends JpaRepository<Fact, UUID> {
  List<Fact> findAllByFactRecord_Id(UUID recordId);

  List<Fact> findAllByFactRecord_IdOrderByFieldKeyAsc(UUID recordId);

  List<Fact> findAllByExtractionRunId(UUID extractionRunId);

  @Query("select f from Fact f join fetch f.factRecord r "
      + "where r.baseVersion.id = :baseVersionId "
      + "order by r.category asc, r.displayOrder asc, f.fieldKey asc")
  List<Fact> findAllByBaseVersionId(@Param("baseVersionId") UUID baseVersionId);
}
