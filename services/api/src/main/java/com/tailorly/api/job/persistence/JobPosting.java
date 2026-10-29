package com.tailorly.api.job.persistence;

import java.time.Instant;
import java.util.UUID;

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

/**
 * Uma vaga de um owner, identificada pela origem ({@code source_key}): o ID da plataforma ou o link
 * canônico, ou o hash do texto colado. As leituras sucessivas ficam em {@link JobPostingVersion};
 * título, empresa e prazo aqui são cópias da versão mais recente, só para listagem.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "job_postings", uniqueConstraints = @UniqueConstraint(
    name = "uk_job_postings_owner_source", columnNames = {"owner_id", "source_key"}))
public class JobPosting {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @Column(name = "source_kind", nullable = false)
  private String sourceKind;

  @Column(name = "source_key", nullable = false, length = 1100)
  private String sourceKey;

  @Column(name = "title", length = 512)
  private String title;

  @Column(name = "company_name", length = 512)
  private String companyName;

  @Column(name = "expires_at", length = 64)
  private String expiresAt;

  @Column(name = "latest_version_number", nullable = false)
  private int latestVersionNumber;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
