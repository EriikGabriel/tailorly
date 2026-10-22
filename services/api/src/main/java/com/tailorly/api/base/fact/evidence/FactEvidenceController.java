package com.tailorly.api.base.fact.evidence;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.evidence.dto.CreateFactEvidenceRequest;
import com.tailorly.api.base.fact.evidence.dto.FactEvidenceResponse;
import com.tailorly.api.base.fact.evidence.dto.UpdateEvidenceLocatorRequest;
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
@RequestMapping({"/api/fact-evidence", "/v1/fact-evidence"})
@RequiredArgsConstructor
public class FactEvidenceController {
  private final FactEvidenceService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FactEvidenceResponse create(@Valid @RequestBody CreateFactEvidenceRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public FactEvidenceResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @GetMapping("/fact/{factId}")
  public List<FactEvidenceResponse> findByFactId(@PathVariable UUID factId) {
    return service.findByFactId(factId);
  }

  @PatchMapping("/{id}/locator")
  public FactEvidenceResponse updateLocator(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateEvidenceLocatorRequest request
  ) {
    return service.updateLocator(id, request);
  }
}
