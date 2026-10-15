package com.tailorly.api.extraction;

import java.util.UUID;

import com.tailorly.api.extraction.dto.CreateExtractionRunRequest;
import com.tailorly.api.extraction.dto.ExtractionRunResponse;
import com.tailorly.api.extraction.dto.FailExtractionRunRequest;
import com.tailorly.api.extraction.dto.UpdateExtractionRunStatusRequest;
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
@RequestMapping({"/api/extraction-runs", "/v1/extraction-runs"})
@RequiredArgsConstructor
public class ExtractionRunController {
  private final ExtractionRunService extractionRunService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExtractionRunResponse create(@Valid @RequestBody CreateExtractionRunRequest request) {
    return extractionRunService.create(request);
  }

  @GetMapping("/{id}")
  public ExtractionRunResponse findById(@PathVariable UUID id) {
    return extractionRunService.findById(id);
  }

  @PatchMapping("/{id}/status")
  public ExtractionRunResponse updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateExtractionRunStatusRequest request
  ) {
    return extractionRunService.updateStatus(id, request);
  }

  @PatchMapping("/{id}/fail")
  public ExtractionRunResponse fail(
      @PathVariable UUID id,
      @Valid @RequestBody FailExtractionRunRequest request
  ) {
    return extractionRunService.fail(id, request);
  }
}
