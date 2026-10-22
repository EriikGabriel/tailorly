package com.tailorly.api.base.fact;

import java.util.UUID;

import com.tailorly.api.base.fact.dto.CreateFactRequest;
import com.tailorly.api.base.fact.dto.FactResponse;
import com.tailorly.api.base.fact.dto.UpdateFactReviewStateRequest;
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
@RequestMapping({"/api/facts", "/v1/facts"})
@RequiredArgsConstructor
public class FactController {
  private final FactService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FactResponse create(@Valid @RequestBody CreateFactRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public FactResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PatchMapping("/{id}/review-state")
  public FactResponse updateReviewState(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateFactReviewStateRequest request
  ) {
    return service.updateReviewState(id, request);
  }

  @PatchMapping("/{id}/confirm")
  public FactResponse confirm(@PathVariable UUID id) {
    return service.confirm(id);
  }
}
