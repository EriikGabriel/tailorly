package com.tailorly.api.base.fact.group;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.fact.Fact;
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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "fact_records")
public class FactRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "base_version_id", nullable = false)
  private BaseVersion baseVersion;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "factRecord", fetch = FetchType.LAZY)
  private Set<Fact> facts = new HashSet<>();
}
