package com.tailorly.api.base;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(
    name = "base_documents",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_base_documents_owner_content_hash",
        columnNames = {"owner_id", "content_hash"}
    )
)
public class BaseDocument {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "original_object_id", nullable = false, unique = true)
  private StoredObject originalObject;

  @Column(name = "content_hash", nullable = false)
  private String contentHash;

  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Column(name = "page_count")
  private Integer pageCount;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ToString.Exclude
  @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
  private Set<BaseVersion> versions = new HashSet<>();
}
