package com.tailorly.api.shared.util;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cache em memória com validade curta, local à instância. Serve às consultas que se repetem a cada
 * requisição (autenticação) e custam uma ida e volta ao banco remoto. Quem altera o dado em cache
 * chama {@link #invalidate} ou {@link #clear}; a validade limita o atraso quando outra instância
 * altera o dado.
 */
public class TtlCache<K, V> {
  private static final int SWEEP_THRESHOLD = 10_000;

  private record Entry<V>(V value, long expiresAt) {
  }

  private final long ttlNanos;
  private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();

  public TtlCache(Duration ttl) {
    this.ttlNanos = ttl.toNanos();
  }

  /** Devolve o valor em cache ou o calcula; um resultado vazio nunca é guardado. */
  public Optional<V> get(K key, Function<K, Optional<V>> loader) {
    long now = System.nanoTime();
    Entry<V> cached = entries.get(key);
    if (cached != null && now - cached.expiresAt < 0) {
      return Optional.of(cached.value);
    }
    Optional<V> loaded = loader.apply(key);
    if (loaded.isPresent()) {
      if (entries.size() > SWEEP_THRESHOLD) {
        entries.values().removeIf(entry -> now - entry.expiresAt >= 0);
      }
      entries.put(key, new Entry<>(loaded.get(), now + ttlNanos));
    } else {
      entries.remove(key);
    }
    return loaded;
  }

  public void invalidate(K key) {
    entries.remove(key);
  }

  public void clear() {
    entries.clear();
  }
}
