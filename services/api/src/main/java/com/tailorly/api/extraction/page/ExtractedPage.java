package com.tailorly.api.extraction.page;

import java.util.UUID;

import com.tailorly.api.extraction.run.ExtractionRun;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.base.fact.evidence.FactEvidence;
import java.util.HashSet;
import java.util.Set;
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
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Texto de uma página extraída de uma {@link ExtractionRun}, com o método usado
 * ({@code DIRECT} ou {@code OCR}) e a referência ao objeto de texto gravado no storage.
 */
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

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "extractedPage", fetch = FetchType.LAZY)
  private Set<FactEvidence> evidence = new HashSet<>();
}
