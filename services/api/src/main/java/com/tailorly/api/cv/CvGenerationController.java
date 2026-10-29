package com.tailorly.api.cv;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.cv.dto.CvGenerationResponse;
import com.tailorly.api.cv.dto.GenerateCvRequest;
import com.tailorly.api.cv.dto.PreviewCvRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cv")
@RequiredArgsConstructor
/** Geração, histórico, download e exclusão de currículos gerados do snapshot aprovado. */
public class CvGenerationController {
  private final CvGenerationService service;

  /** {@code 201} para um currículo novo, {@code 200} quando a entrada era idêntica a uma geração existente. */
  @PostMapping("/generate")
  public ResponseEntity<CvGenerationResponse> generate(@Valid @RequestBody GenerateCvRequest request) {
    CvGenerationService.Result result = service.generate(request);
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.generation());
  }

  @GetMapping("/generations/owner/{ownerId}")
  public List<CvGenerationResponse> findByOwner(@PathVariable UUID ownerId) {
    return service.findByOwner(ownerId);
  }

  @GetMapping("/generations/{id}")
  public CvGenerationResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  /** {@code format}: {@code pdf} (padrão) ou {@code markdown}. */
  @GetMapping("/generations/{id}/download")
  public ResponseEntity<byte[]> download(@PathVariable UUID id, @RequestParam(defaultValue = "pdf") String format) {
    CvGenerationService.Download file = service.download(id, format);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.filename()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(file.content());
  }

  /**
   * Prévia ao vivo: compila o código Typst do corpo com o conteúdo final desta geração e devolve o
   * PDF. Não grava nada nem consome cota; erros de compilação voltam como {@code 422} com
   * diagnósticos em {@code details}.
   */
  @PostMapping("/generations/{id}/preview")
  public ResponseEntity<byte[]> preview(@PathVariable UUID id, @Valid @RequestBody PreviewCvRequest request) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(service.preview(id, request));
  }

  @DeleteMapping("/generations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
