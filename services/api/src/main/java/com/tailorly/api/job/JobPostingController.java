package com.tailorly.api.job;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.job.dto.CreateJobPostingRequest;
import com.tailorly.api.job.dto.ExtractJobPostingRequest;
import com.tailorly.api.job.dto.JobPostingDetailResponse;
import com.tailorly.api.job.dto.JobPostingResponse;
import com.tailorly.api.job.dto.JobPostingSummaryResponse;
import com.tailorly.api.job.dto.JobPostingVersionResponse;
import com.tailorly.api.job.reader.JobPostingExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/job-postings")
@RequiredArgsConstructor
public class JobPostingController {
  private final JobPostingExtractionService extractionService;
  private final JobPostingService service;

  /** Pré-visualização: lê o link e devolve os detalhes sem gravar nem consultar a IA. */
  @PostMapping("/extract")
  public JobPostingResponse extract(@Valid @RequestBody ExtractJobPostingRequest request) {
    return extractionService.extract(request.url());
  }

  /** Lê, grava e analisa a vaga: {@code 201} para uma versão nova, {@code 200} quando o conteúdo já existia. */
  @PostMapping
  public ResponseEntity<JobPostingVersionResponse> create(@Valid @RequestBody CreateJobPostingRequest request) {
    JobPostingService.CreateResult result = service.create(request);
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.version());
  }

  @GetMapping("/owner/{ownerId}")
  public List<JobPostingSummaryResponse> findByOwner(@PathVariable UUID ownerId) {
    return service.findByOwner(ownerId);
  }

  @GetMapping("/{id}")
  public JobPostingDetailResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @GetMapping("/{id}/versions/{versionNumber}")
  public JobPostingVersionResponse findVersion(@PathVariable UUID id, @PathVariable int versionNumber) {
    return service.findVersion(id, versionNumber);
  }

  @PostMapping("/{id}/versions/{versionNumber}/analysis")
  public JobPostingVersionResponse reanalyze(@PathVariable UUID id, @PathVariable int versionNumber) {
    return service.reanalyze(id, versionNumber);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
