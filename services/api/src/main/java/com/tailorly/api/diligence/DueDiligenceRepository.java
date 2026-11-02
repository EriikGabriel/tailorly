package com.tailorly.api.diligence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DueDiligenceRepository extends JpaRepository<DueDiligence, UUID> {
  List<DueDiligence> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  /** Análises recentes da mesma empresa (sem diferenciar maiúsculas), para a esteira não repetir a consulta. */
  @org.springframework.data.jpa.repository.Query("""
      select d from DueDiligence d where d.owner.id = :ownerId and lower(d.companyName) = lower(:company)
        and d.status <> 'failed' and d.createdAt >= :since order by d.createdAt desc""")
  List<DueDiligence> findRecentByCompany(UUID ownerId, String company, java.time.Instant since);
}
