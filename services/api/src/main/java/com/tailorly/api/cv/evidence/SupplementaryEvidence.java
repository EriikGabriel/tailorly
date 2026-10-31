package com.tailorly.api.cv.evidence;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.cv.CvGeneration;
import com.tailorly.api.owner.Owner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Um detalhe encontrado fora dos fatos aprovados, com a fonte e o trecho que o sustenta. Vinculado
 * ao snapshot do Ground Truth; a geração só aparece depois que a evidência é usada. Apagar o
 * snapshot ou a geração apaga a evidência (ver V13).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "cv_supplementary_evidence")
public class SupplementaryEvidence {
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
  @JoinColumn(name = "snapshot_id", nullable = false)
  private GtSnapshot snapshot;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "generation_id")
  private CvGeneration generation;

  /** {@code base_link}, {@code web} ou {@code vault}; ver V13. */
  @Column(name = "source_kind", nullable = false, length = 32)
  private String sourceKind;

  @Column(name = "source_url", length = 2048)
  private String sourceUrl;

  /** Link do projeto ao qual a evidência se refere; é por ele que o enriquecimento casa com o projeto. */
  @Column(name = "subject_url", length = 2048)
  private String subjectUrl;

  @Column(name = "source_excerpt", nullable = false, columnDefinition = "text")
  private String sourceExcerpt;

  /** Afirmação que a evidência sustenta, escrita em uma frase curta. */
  @Column(name = "claim", nullable = false, columnDefinition = "text")
  private String claim;

  /** {@code pendente}, {@code confirmado} ou {@code descartado}; só o confirmado entra no currículo. */
  @Column(name = "review_state", nullable = false, length = 16)
  private String reviewState;

  @Column(name = "collected_at", nullable = false)
  private Instant collectedAt;
}
