package com.tailorly.api.base.document;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.document.dto.BaseDocumentResponse;
import com.tailorly.api.base.document.dto.BaseDocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

/**
 * Endpoints HTTP do Currículo Base enquanto arquivo: upload e consultas por id, owner ou hash.
 * As transições de versão (candidata, extraída, ativa) pertencem a {@link BaseVersionController}.
 */
@RestController
@RequestMapping({"/api/base-documents", "/v1/base-documents"})
@RequiredArgsConstructor
public class BaseDocumentController {
  private final BaseDocumentService baseDocumentService;
  private final BaseDocumentUploadService uploadService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public BaseDocumentUploadResponse upload(
      @RequestParam UUID ownerId,
      @RequestPart MultipartFile file
  ) {
    return uploadService.upload(ownerId, file);
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
