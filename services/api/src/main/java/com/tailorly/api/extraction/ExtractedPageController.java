package com.tailorly.api.extraction;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.extraction.dto.CreateExtractedPageRequest;
import com.tailorly.api.extraction.dto.ExtractedPageResponse;
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

@RestController
@RequestMapping({"/api/extracted-pages", "/v1/extracted-pages"})
@RequiredArgsConstructor
public class ExtractedPageController {
  private final ExtractedPageService extractedPageService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExtractedPageResponse create(@Valid @RequestBody CreateExtractedPageRequest request) {
    return extractedPageService.create(request);
  }

  @GetMapping("/{id}")
  public ExtractedPageResponse findById(@PathVariable UUID id) {
    return extractedPageService.findById(id);
  }

  @GetMapping("/run/{extractionRunId}")
  public List<ExtractedPageResponse> findAllByRunId(@PathVariable UUID extractionRunId) {
    return extractedPageService.findAllByRunId(extractionRunId);
  }

  @GetMapping("/hash/{textHash}")
  public ExtractedPageResponse findByTextHash(@PathVariable String textHash) {
    return extractedPageService.findByTextHash(textHash);
  }
}
