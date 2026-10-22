package com.tailorly.api.base.review;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.fact.Fact;
import com.tailorly.api.base.version.BaseVersion;
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
@Table(name = "review_issues")
public class ReviewIssue {
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

  @Column(name = "job_version_id")
  private UUID jobVersionId;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "reason", nullable = false, columnDefinition = "text")
  private String reason;

  @Column(name = "blocking", nullable = false)
  private boolean blocking;

  @Column(name = "status", nullable = false)
  private String status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "question_and_choices", columnDefinition = "jsonb")
  private Map<String, Object> questionAndChoices;

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
  private Set<ReviewDecision> decisions = new HashSet<>();
}
