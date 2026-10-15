package com.tailorly.api.base;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.dto.BaseDocumentResponse;
import com.tailorly.api.base.dto.CreateBaseDocumentRequest;
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
@RequestMapping({"/api/base-documents", "/v1/base-documents"})
@RequiredArgsConstructor
public class BaseDocumentController {
  private final BaseDocumentService baseDocumentService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BaseDocumentResponse create(@Valid @RequestBody CreateBaseDocumentRequest request) {
    return baseDocumentService.create(request);
  }

  @GetMapping("/{id}")
  public BaseDocumentResponse findById(@PathVariable UUID id) {
    return baseDocumentService.findById(id);
  }

  @GetMapping("/owner/{ownerId}")
  public List<BaseDocumentResponse> findByOwnerId(@PathVariable UUID ownerId) {
    return baseDocumentService.findAllByOwnerId(ownerId);
  }

  @GetMapping("/hash/{contentHash}")
  public BaseDocumentResponse findByContentHash(@PathVariable String contentHash) {
    return baseDocumentService.findByContentHash(contentHash);
  }

}
