package com.tailorly.api.features;

import com.tailorly.api.search.WebSearchClient;
import com.tailorly.api.vault.VaultLocations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recursos opcionais que este servidor realmente oferece, para a interface esconder o que não
 * existe no ambiente (por exemplo, vault e busca web em produção sem essa configuração). É público
 * porque visitantes também veem essas telas; só expõe se cada recurso existe, nunca a configuração.
 */
@RestController
@RequestMapping("/v1/features")
@RequiredArgsConstructor
public class FeaturesController {
  private final VaultLocations vaultLocations;
  private final WebSearchClient webSearch;

  /**
   * @param vault     o servidor tem uma raiz de vaults Obsidian configurada
   * @param webSearch o servidor tem um provedor de busca web configurado
   */
  public record Features(boolean vault, boolean webSearch) {
  }

  @GetMapping
  public Features get() {
    return new Features(vaultLocations.isAvailable(), webSearch.isConfigured());
  }
}
