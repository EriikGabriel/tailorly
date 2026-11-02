package com.tailorly.api.diligence;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.diligence.dto.CreateDueDiligenceRequest;
import com.tailorly.api.diligence.dto.DueDiligenceResponse;
import com.tailorly.api.diligence.dto.DueDiligenceSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/due-diligence")
@RequiredArgsConstructor
public class DueDiligenceController {
  private final DueDiligenceService service;

  /** Executa e grava uma nova análise; cada chamada gera um relatório novo no histórico do owner. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DueDiligenceResponse create(@Valid @RequestBody CreateDueDiligenceRequest request) {
    return service.create(request);
  }

  @GetMapping("/owner/{ownerId}")
  public List<DueDiligenceSummaryResponse> findByOwner(@PathVariable UUID ownerId) {
    return service.findByOwner(ownerId);
  }

  @GetMapping("/{id}")
  public DueDiligenceResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
