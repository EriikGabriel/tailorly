package com.tailorly.api.base.snapshot;

import java.util.UUID;

import com.tailorly.api.base.fact.Fact;
import com.tailorly.api.base.fact.group.FactRecord;
import com.tailorly.api.base.review.ReviewDecision;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "gt_snapshot_items")
public class GtSnapshotItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "snapshot_id", nullable = false)
  private GtSnapshot snapshot;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_fact_id", nullable = false)
  private Fact sourceFact;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_decision_id", nullable = false)
  private ReviewDecision sourceDecision;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", nullable = false)
  private FactRecord factRecord;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "field_key", nullable = false)
  private String fieldKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "approved_value", nullable = false, columnDefinition = "jsonb")
  private Object approvedValue;
}
