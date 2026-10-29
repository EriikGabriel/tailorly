package com.tailorly.api.cv.template;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Uma versão imutável de um template Typst: embarcada (global) ou editada por um owner. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "cv_templates")
public class CvTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  /** Nulo nos templates embarcados (globais); preenchido nos editados por uma pessoa. */
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private Owner owner;

  @Column(name = "name", nullable = false, length = 64)
  private String name;

  @Column(name = "version", nullable = false)
  private int version;

  @ToString.Exclude
  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
