package com.tailorly.api.base.fact;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.base.fact.evidence.FactEvidence;
import com.tailorly.api.base.fact.group.FactRecord;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "facts")
public class Fact {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", nullable = false)
  private FactRecord factRecord;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "extraction_run_id")
  private ExtractionRun extractionRun;

  @Column(name = "field_key", nullable = false)
  private String fieldKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extracted_value", columnDefinition = "jsonb")
  private Object extractedValue;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reviewed_value", columnDefinition = "jsonb")
  private Object reviewedValue;

  @Column(name = "origin", nullable = false)
  private String origin;

  @Column(name = "review_state", nullable = false)
  private String reviewState;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "revalidate_after")
  private Instant revalidateAfter;

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "fact", fetch = FetchType.LAZY)
  private Set<FactEvidence> evidence = new HashSet<>();
}
