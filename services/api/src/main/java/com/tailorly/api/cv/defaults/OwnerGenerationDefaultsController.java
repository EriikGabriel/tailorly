package com.tailorly.api.cv.defaults;

import java.util.UUID;

import com.tailorly.api.cv.defaults.dto.GenerationDefaultsResponse;
import com.tailorly.api.cv.defaults.dto.UpdateGenerationDefaultsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Padrões de geração da conta (layout de currículo escolhido). */
@RestController
@RequestMapping("/v1/owners/{ownerId}/generation-defaults")
@RequiredArgsConstructor
public class OwnerGenerationDefaultsController {
  private final OwnerGenerationDefaultsService service;

  @GetMapping
  public GenerationDefaultsResponse get(@PathVariable UUID ownerId) {
    return new GenerationDefaultsResponse(service.templateName(ownerId));
  }

  @PutMapping
  public GenerationDefaultsResponse set(@PathVariable UUID ownerId,
      @Valid @RequestBody UpdateGenerationDefaultsRequest request) {
    return new GenerationDefaultsResponse(service.setTemplateName(ownerId, request.templateName()));
  }
}
