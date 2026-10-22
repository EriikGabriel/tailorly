package com.tailorly.api.base.fact.evidence;

import java.util.UUID;

import com.tailorly.api.extraction.page.ExtractedPage;
import com.tailorly.api.base.fact.Fact;
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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "fact_evidence")
public class FactEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fact_id", nullable = false)
  private Fact fact;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "extracted_page_id", nullable = false)
  private ExtractedPage extractedPage;

  @Column(name = "source_excerpt", nullable = false, columnDefinition = "text")
  private String sourceExcerpt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "source_locator", nullable = false, columnDefinition = "jsonb")
  private Object sourceLocator;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bounding_box", columnDefinition = "jsonb")
  private Object boundingBox;
}
