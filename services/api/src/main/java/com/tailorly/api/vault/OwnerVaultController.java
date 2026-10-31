package com.tailorly.api.vault;

import java.util.UUID;

import com.tailorly.api.vault.dto.UpdateOwnerVaultRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Preferências de segundo cérebro da conta (vault Obsidian e busca web): consulta, definição e remoção do vault. */
@RestController
@RequestMapping("/v1/owners/{ownerId}/vault")
@RequiredArgsConstructor
public class OwnerVaultController {
  private final OwnerVaultService service;

  @GetMapping
  public OwnerVaultService.Settings get(@PathVariable UUID ownerId) {
    return service.get(ownerId);
  }

  @PutMapping
  public OwnerVaultService.Settings set(@PathVariable UUID ownerId,
      @Valid @RequestBody UpdateOwnerVaultRequest request) {
    return service.set(ownerId, request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear(@PathVariable UUID ownerId) {
    service.clear(ownerId);
  }
}
