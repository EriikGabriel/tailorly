package com.tailorly.api.cv;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.cv.model.CvDocument;
import com.tailorly.api.cv.model.CvPreferences;
import com.tailorly.api.cv.model.CvTailoring;
import com.tailorly.api.cv.pipeline.CvAtsResult;
import com.tailorly.api.cv.template.CvTemplate;
import com.tailorly.api.job.persistence.JobPostingVersion;
import com.tailorly.api.objects.StoredObject;
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
 * Um currículo gerado: registra o snapshot do Ground Truth, a versão da vaga e a versão do
 * template usados, e aponta para os artefatos PDF e Markdown no storage privado. Os artefatos
 * contêm dados pessoais e são apagados com a geração ou com o owner.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "cv_generations", uniqueConstraints = @UniqueConstraint(
    name = "uk_cv_generations_owner_input", columnNames = {"owner_id", "input_hash"}))
public class CvGeneration {
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
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_posting_version_id", nullable = false)
  private JobPostingVersion jobPostingVersion;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id", nullable = false)
  private CvTemplate template;

  /** Hash do conteúdo montado e do template: a mesma entrada reaproveita a geração existente. */
  @Column(name = "input_hash", nullable = false, length = 64)
  private String inputHash;

  @Column(name = "renderer_version", nullable = false, length = 128)
  private String rendererVersion;

  @Column(name = "pipeline_version", nullable = false, length = 32)
  private String pipelineVersion;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "preferences", nullable = false, columnDefinition = "jsonb")
  private CvPreferences preferences;

  /** O conteúdo final enviado ao template: permite compilar uma prévia com outro código Typst. */
  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "document", nullable = false, columnDefinition = "jsonb")
  private CvDocument document;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ats", nullable = false, columnDefinition = "jsonb")
  private CvAtsResult ats;

  @ToString.Exclude
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tailoring", nullable = false, columnDefinition = "jsonb")
  private CvTailoring tailoring;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pdf_object_id", nullable = false)
  private StoredObject pdfObject;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "markdown_object_id", nullable = false)
  private StoredObject markdownObject;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
