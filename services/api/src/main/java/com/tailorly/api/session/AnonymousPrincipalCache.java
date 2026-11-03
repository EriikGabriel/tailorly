package com.tailorly.api.session;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import com.tailorly.api.shared.util.TtlCache;
import org.springframework.stereotype.Component;

/**
 * Sessões anônimas já validadas, por hash do token, com validade curta, para não consultar o banco
 * a cada requisição. A expiração é sempre reconferida por quem usa; revogar ou apagar uma sessão
 * limpa o cache.
 */
@Component
public class AnonymousPrincipalCache {
  private final TtlCache<String, AnonymousPrincipal> cache = new TtlCache<>(Duration.ofSeconds(15));

  public Optional<AnonymousPrincipal> get(String tokenHash,
      Function<String, Optional<AnonymousPrincipal>> loader) {
    return cache.get(tokenHash, loader);
  }

  public void clear() {
    cache.clear();
  }
}
