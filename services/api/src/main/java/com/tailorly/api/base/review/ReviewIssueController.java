package com.tailorly.api.base.review;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.dto.UpdateFactReviewStateRequest;
import com.tailorly.api.base.review.dto.ReviewIssueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/review-issues", "/v1/review-issues"})
@RequiredArgsConstructor
public class ReviewIssueController {
  private final ReviewIssueService service;

  @GetMapping("/version/{baseVersionId}")
  public List<ReviewIssueResponse> findByVersion(@PathVariable UUID baseVersionId) {
    return service.findByVersion(baseVersionId);
  }

  @PatchMapping("/{id}/answer")
  public ReviewIssueResponse answer(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateFactReviewStateRequest request
  ) {
    return service.answer(id, request);
  }
}
