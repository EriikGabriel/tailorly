package com.tailorly.api.cv;

import java.util.UUID;

import com.tailorly.api.cv.dto.CvTemplateResponse;
import com.tailorly.api.cv.dto.SaveCvTemplateRequest;
import com.tailorly.api.cv.template.CvTemplateService;
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
@RequestMapping("/v1/cv/templates")
@RequiredArgsConstructor
/** Consulta do template embarcado e edição versionada do template de cada owner. */
public class CvTemplateController {
  private final CvTemplateService service;

  /** A versão mais recente do template embarcado, igual para todos. */
  @GetMapping("/bundled")
  public CvTemplateResponse bundled() {
    return service.bundled();
  }

  /** O template que as próximas gerações do owner usam: a última versão editada ou o embarcado. */
  @GetMapping("/owner/{ownerId}/current")
  public CvTemplateResponse current(@PathVariable UUID ownerId) {
    return service.current(ownerId);
  }

  @GetMapping("/{id}")
  public CvTemplateResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  /** Salva o código como a próxima versão do template do owner; o mesmo conteúdo não cria versão nova. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CvTemplateResponse save(@Valid @RequestBody SaveCvTemplateRequest request) {
    return service.save(request.ownerId(), request.content());
  }
}
