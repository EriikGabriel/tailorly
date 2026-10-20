package com.tailorly.api.extraction.page;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.extraction.page.dto.ExtractedPageResponse;
import com.tailorly.api.extraction.page.dto.ExtractedPageTextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de páginas extraídas e do texto UTF-8 gravado para cada uma. */
@RestController
@RequestMapping({"/api/extracted-pages", "/v1/extracted-pages"})
@RequiredArgsConstructor
public class ExtractedPageController {
  private final ExtractedPageService extractedPageService;

  @GetMapping("/{id}/text")
  public ExtractedPageTextResponse readText(@PathVariable UUID id) {
    return new ExtractedPageTextResponse(id, extractedPageService.readText(id));
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
