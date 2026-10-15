package com.tailorly.api.extraction;

import java.util.UUID;

import com.tailorly.api.objects.StoredObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "extracted_pages")
public class ExtractedPage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "extraction_run_id", nullable = false)
  private ExtractionRun extractionRun;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "extraction_method", nullable = false)
  private String extractionMethod;

  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "text_object_id", nullable = false, unique = true)
  private StoredObject textObject;

  @Column(name = "text_hash", nullable = false)
  private String textHash;
}
