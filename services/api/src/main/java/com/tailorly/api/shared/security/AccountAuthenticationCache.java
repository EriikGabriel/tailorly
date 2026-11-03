package com.tailorly.api.shared.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.tailorly.api.shared.config.SecurityConfiguration.CachedAccount;
import com.tailorly.api.shared.util.TtlCache;
import org.springframework.stereotype.Component;

/**
 * Contas já autenticadas por JWT, com validade curta, para não consultar o banco a cada requisição.
 * Desativar a conta ou mudar seus papéis invalida a entrada de imediato nesta instância.
 */
@Component
public class AccountAuthenticationCache {
  private final TtlCache<UUID, CachedAccount> cache = new TtlCache<>(Duration.ofSeconds(15));

  public Optional<CachedAccount> get(UUID userId, Function<UUID, Optional<CachedAccount>> loader) {
    return cache.get(userId, loader);
  }

  public void invalidate(UUID userId) {
    cache.invalidate(userId);
  }
}
