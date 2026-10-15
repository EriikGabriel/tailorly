package com.tailorly.api.base;

import java.util.UUID;

import com.tailorly.api.base.dto.BaseVersionResponse;
import com.tailorly.api.base.dto.CreateBaseVersionRequest;
import com.tailorly.api.base.dto.UpdateBaseVersionStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/base-versions", "/v1/base-versions"})
@RequiredArgsConstructor
public class BaseVersionController {
  private final BaseVersionService baseVersionService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BaseVersionResponse create(@Valid @RequestBody CreateBaseVersionRequest request) {
    return baseVersionService.create(request);
  }

  @GetMapping("/{id}")
  public BaseVersionResponse findById(@PathVariable UUID id) {
    return baseVersionService.findById(id);
  }

  @PatchMapping("/{id}/status")
  public BaseVersionResponse updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateBaseVersionStatusRequest request
  ) {
    return baseVersionService.updateStatus(id, request);
  }

  @PatchMapping("/{id}/increment-review-revision")
  public BaseVersionResponse incrementReviewRevision(@PathVariable UUID id) {
    return baseVersionService.incrementReviewRevision(id);
  }
}
