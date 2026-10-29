package com.tailorly.api.cv.dto;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.cv.template.CvTemplate;

/** {@code custom} é verdadeiro nos templates editados por uma pessoa; os embarcados são globais. */
public record CvTemplateResponse(UUID id, String name, int version, boolean custom, String contentHash,
    String content, Instant createdAt) {
  public static CvTemplateResponse from(CvTemplate template) {
    return new CvTemplateResponse(template.getId(), template.getName(), template.getVersion(),
        template.getOwner() != null, template.getContentHash(), template.getContent(), template.getCreatedAt());
  }
}
