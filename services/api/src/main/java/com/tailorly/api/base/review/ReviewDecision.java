package com.tailorly.api.base.review;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.fact.Fact;
import com.tailorly.api.base.version.BaseVersion;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "review_decisions")
public class ReviewDecision {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "base_version_id", nullable = false)
  private BaseVersion baseVersion;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fact_id")
  private Fact fact;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "issue_id")
  private ReviewIssue issue;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_owner_id", nullable = false)
  private Owner actorOwner;

  @Column(name = "result", nullable = false)
  private String decisionResult;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "previous_value", columnDefinition = "jsonb")
  private Object previousValue;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "approved_value", columnDefinition = "jsonb")
  private Object approvedValue;

  @Column(name = "review_revision", nullable = false)
  private int reviewRevision;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;
}
