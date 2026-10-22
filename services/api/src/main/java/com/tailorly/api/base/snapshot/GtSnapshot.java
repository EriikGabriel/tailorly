package com.tailorly.api.base.snapshot;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "gt_snapshots", uniqueConstraints = @UniqueConstraint(
    name = "uk_gt_snapshots_owner_number", columnNames = {"owner_id", "snapshot_number"}
))
public class GtSnapshot {
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
  @JoinColumn(name = "base_version_id", nullable = false)
  private BaseVersion baseVersion;

  @Column(name = "snapshot_number", nullable = false)
  private int snapshotNumber;

  @Column(name = "approved_review_revision", nullable = false)
  private int approvedReviewRevision;

  @Column(name = "content_hash", nullable = false)
  private String contentHash;

  @Column(name = "approved_at", nullable = false)
  private Instant approvedAt;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "snapshot", fetch = FetchType.LAZY)
  private Set<GtSnapshotItem> items = new HashSet<>();
}
