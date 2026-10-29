package com.tailorly.api.job.persistence;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.job.dto.JobAnalysis;
import com.tailorly.api.job.dto.JobPostingResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Leitura imutável de uma vaga: o conteúdo ({@code details}) nunca muda depois de gravado, para que
 * uma geração possa registrar exatamente a versão da vaga que usou. Só a análise por IA, que pode
 * ser refeita, e {@code lastCheckedAt}, atualizado quando o mesmo conteúdo é lido de novo, mudam.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "job_posting_versions", uniqueConstraints = @UniqueConstraint(
    name = "uk_job_posting_versions_number", columnNames = {"job_posting_id", "version_number"}))
public class JobPostingVersion {
  public static final String COMPLETED = "completed";
  public static final String UNAVAILABLE = "unavailable";
  public static final String FAILED = "failed";
  public static final String QUOTA_EXCEEDED = "quota_exceeded";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_posting_id", nullable = false)
  private JobPosting jobPosting;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = false, columnDefinition = "jsonb")
  private JobPostingResponse details;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "analysis", columnDefinition = "jsonb")
  private JobAnalysis analysis;

  @Column(name = "analysis_status", nullable = false, length = 32)
  private String analysisStatus;

  @Column(name = "analyzer_version", length = 64)
  private String analyzerVersion;

  @Column(name = "analyzed_at")
  private Instant analyzedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_checked_at", nullable = false)
  private Instant lastCheckedAt;
}
