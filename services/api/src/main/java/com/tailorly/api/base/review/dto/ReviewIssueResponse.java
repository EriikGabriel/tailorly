package com.tailorly.api.base.review.dto;

import java.util.Map;
import java.util.UUID;

import com.tailorly.api.base.fact.dto.FactResponse;
import com.tailorly.api.base.review.ReviewIssue;
import lombok.Builder;

@Builder
public record ReviewIssueResponse(
    UUID id,
    UUID baseVersionId,
    UUID factId,
    String category,
    String reason,
    boolean blocking,
    String status,
    Map<String, Object> questionAndChoices,
    FactResponse fact
) {
  public static ReviewIssueResponse from(ReviewIssue issue) {
    return ReviewIssueResponse.builder()
        .id(issue.getId())
        .baseVersionId(issue.getBaseVersion().getId())
        .factId(issue.getFact() == null ? null : issue.getFact().getId())
        .category(issue.getCategory())
        .reason(issue.getReason())
        .blocking(issue.isBlocking())
        .status(issue.getStatus())
        .questionAndChoices(issue.getQuestionAndChoices())
        .fact(issue.getFact() == null ? null : FactResponse.from(issue.getFact()))
        .build();
  }
}
