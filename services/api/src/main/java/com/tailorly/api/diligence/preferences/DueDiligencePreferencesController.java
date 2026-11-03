package com.tailorly.api.diligence.preferences;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Preferências da conta sobre a due diligence automática. */
@RestController
@RequestMapping("/v1/owners/{ownerId}/due-diligence-preferences")
@RequiredArgsConstructor
public class DueDiligencePreferencesController {
  private final DueDiligencePreferencesService service;

  @GetMapping
  public DueDiligencePreferencesResponse get(@PathVariable UUID ownerId) {
    return service.get(ownerId);
  }

  @PutMapping
  public DueDiligencePreferencesResponse set(@PathVariable UUID ownerId,
      @RequestBody UpdateDueDiligencePreferencesRequest request) {
    return service.update(ownerId, request);
  }
}
