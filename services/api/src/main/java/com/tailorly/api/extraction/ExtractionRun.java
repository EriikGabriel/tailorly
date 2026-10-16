package com.tailorly.api.extraction;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.BaseVersion;
import com.tailorly.api.objects.StoredObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "extraction_runs")
public class ExtractionRun {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "base_version_id", nullable = false)
  private BaseVersion baseVersion;

  @Column(name = "pipeline_fingerprint", nullable = false)
  private String pipelineFingerprint;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "pipeline_versions", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> pipelineVersions;

  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "output_object_id", unique = true)
  private StoredObject outputObject;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "error_code")
  private String errorCode;

  @ToString.Exclude
  @Builder.Default
  @OneToMany(mappedBy = "extractionRun", fetch = FetchType.LAZY)
  private Set<ExtractedPage> pages = new HashSet<>();
}
