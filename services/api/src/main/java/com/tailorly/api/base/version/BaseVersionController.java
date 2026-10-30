package com.tailorly.api.base.version;

import java.util.UUID;

import com.tailorly.api.base.version.dto.BaseVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta e exclusão de versões do Currículo Base por id. As rotas públicas de criação,
 * alteração livre de status e incremento de revisão foram removidas: número e status pertencem
 * ao servidor.
 */
@RestController
@RequestMapping({"/api/base-versions", "/v1/base-versions"})
@RequiredArgsConstructor
public class BaseVersionController {
  private final BaseVersionService baseVersionService;
  private final BaseVersionDeletionService deletionService;

  @GetMapping("/{id}")
  public BaseVersionResponse findById(@PathVariable UUID id) {
    return baseVersionService.findById(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    deletionService.delete(id);
  }
}
