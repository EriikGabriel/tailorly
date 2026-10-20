package com.tailorly.api.base.version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.document.BaseDocument;
import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.owner.Owner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Uma versão do Currículo Base de um owner: candidata, extraída ou ativa.
 *
 * <p>Número e status pertencem ao servidor (não há rota pública para defini-los livremente).
 * Cada owner tem no máximo uma versão ativa por vez (apontada por
 * {@code owner.active_snapshot_id}), mas versões anteriores permanecem para auditoria e para
 * rastrear a origem de currículos já gerados. {@code reviewRevision} e
 * {@code temporalReviewRequired} ainda são placeholders para a revisão de fatos do MER 2; hoje
 * apenas sinalizam que um reenvio idêntico pode exigir nova confirmação de fatos sensíveis ao
 * tempo (como vínculo de emprego atual), sem herdar respostas automaticamente.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(
    name = "base_versions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_base_versions_owner_version_number",
        columnNames = {"owner_id", "version_number"}
    )
)
public class BaseVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private BaseDocument document;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "review_revision", nullable = false)
  private int reviewRevision;

  @Column(name = "temporal_review_required", nullable = false)
  private boolean temporalReviewRequired;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ToString.Exclude
  @Builder.Default
  @OneToMany(mappedBy = "baseVersion", fetch = FetchType.LAZY)
  private Set<ExtractionRun> extractionRuns = new HashSet<>();
}
