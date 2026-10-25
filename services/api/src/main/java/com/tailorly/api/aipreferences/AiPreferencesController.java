package com.tailorly.api.aipreferences;

import com.tailorly.api.aipreferences.dto.AiPreferencesResponse;
import com.tailorly.api.aipreferences.dto.UpdateAiPreferencesRequest;
import com.tailorly.api.shared.security.CurrentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preferência de IA da conta autenticada para a extração do Currículo Base. Sessões anônimas
 * não têm acesso: só contas autenticadas têm um owner estável o suficiente para guardar
 * credenciais de provedores de IA.
 */
@RestController
@RequestMapping("/v1/ai-preferences")
@RequiredArgsConstructor
public class AiPreferencesController {
  private final AiPreferencesService aiPreferencesService;
  private final CurrentAccountService currentAccountService;

  @GetMapping
  public AiPreferencesResponse current(Authentication authentication) {
    var account = currentAccountService.findOrCreate(authentication);
    return aiPreferencesService.find(account.owner().id());
  }

  @PutMapping
  public AiPreferencesResponse update(Authentication authentication,
      @Valid @RequestBody UpdateAiPreferencesRequest request) {
    var account = currentAccountService.findOrCreate(authentication);
    return aiPreferencesService.save(account.owner().id(), request);
  }
}
