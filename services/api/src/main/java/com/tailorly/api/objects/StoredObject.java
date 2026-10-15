package com.tailorly.api.objects;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.base.BaseDocument;
import com.tailorly.api.extraction.ExtractedPage;
import com.tailorly.api.extraction.ExtractionRun;
import com.tailorly.api.owner.Owner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "stored_objects")
public class StoredObject {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @Column(name = "purpose", nullable = false)
  private String purpose;

  @Column(name = "storage_key", nullable = false, unique = true)
  private String storageKey;

  @Column(name = "content_hash", nullable = false)
  private String contentHash;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "deletion_status", nullable = false)
  private String deletionStatus;

  @OneToOne(mappedBy = "originalObject")
  private BaseDocument baseDocument;

  @OneToOne(mappedBy = "outputObject")
  private ExtractionRun extractionRun;

  @OneToOne(mappedBy = "textObject")
  private ExtractedPage extractedPage;
}
