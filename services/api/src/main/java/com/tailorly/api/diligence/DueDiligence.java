package com.tailorly.api.diligence;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.diligence.dto.DueDiligenceReport;
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

/**
 * Uma due diligence concluída, guardada na conta (ou na sessão anônima) de quem a pediu. Não referencia
 * fatos nem snapshots do Ground Truth: a análise apoia a decisão da pessoa e não os modifica.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "company_due_diligence")
public class DueDiligence {
  public static final String RUNNING = "running";
  public static final String COMPLETED = "completed";
  public static final String FAILED = "failed";
  public static final String MANUAL = "manual";
  public static final String PIPELINE = "pipeline";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @Column(name = "company_name", nullable = false, length = 200)
  private String companyName;

  @Column(name = "job_posting_id")
  private UUID jobPostingId;

  @Column(name = "job_version_number")
  private Integer jobVersionNumber;

  @Column(name = "analyzer_version", nullable = false, length = 64)
  private String analyzerVersion;

  /** {@code running}, {@code completed} ou {@code failed}; só {@code completed} tem relatório. */
  @Column(name = "status", nullable = false, length = 16)
  private String status;

  /** {@code manual} (pedido da pessoa) ou {@code pipeline} (disparo automático depois do Ground Truth). */
  @Column(name = "trigger_kind", nullable = false, length = 16)
  private String triggerKind;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "report", columnDefinition = "jsonb")
  private DueDiligenceReport report;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
