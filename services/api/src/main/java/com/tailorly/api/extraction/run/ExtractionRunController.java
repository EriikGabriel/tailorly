package com.tailorly.api.extraction.run;

import java.util.UUID;
import java.util.List;

import com.tailorly.api.extraction.run.dto.CreateExtractionRunRequest;
import com.tailorly.api.extraction.run.dto.ExtractionRunResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solicita e consulta execuções de extração. {@link #create} aceita apenas o id da versão e
 * devolve {@code 202} com uma execução nova ou reaproveitada; use-o para tentar novamente após
 * uma falha. Fingerprint, versões do pipeline, status e resultados são definidos pelo servidor.
 */
@RestController
@RequestMapping({"/api/extraction-runs", "/v1/extraction-runs"})
@RequiredArgsConstructor
public class ExtractionRunController {
  private final ExtractionRunService extractionRunService;

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ExtractionRunResponse create(@Valid @RequestBody CreateExtractionRunRequest request) {
    return extractionRunService.create(request);
  }

  @GetMapping("/{id}")
  public ExtractionRunResponse findById(@PathVariable UUID id) {
    return extractionRunService.findById(id);
  }

  @GetMapping("/version/{baseVersionId}")
  public List<ExtractionRunResponse> findByVersion(@PathVariable UUID baseVersionId) {
    return extractionRunService.findByVersion(baseVersionId);
  }
}
