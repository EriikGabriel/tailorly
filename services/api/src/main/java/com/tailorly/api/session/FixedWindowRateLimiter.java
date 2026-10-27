package com.tailorly.api.session;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limitador de janela fixa em memória, por chave (sessão ou IP). É local à instância da API:
 * com várias instâncias o limite efetivo é multiplicado pelo número delas.
 */
public class FixedWindowRateLimiter {
  private static final int SWEEP_THRESHOLD = 10_000;

  private final int limit;
  private final long windowMillis;
  private final LongSupplier clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public FixedWindowRateLimiter(int limit, Duration window) {
    this(limit, window, System::currentTimeMillis);
  }

  FixedWindowRateLimiter(int limit, Duration window, LongSupplier clock) {
    this.limit = limit;
    this.windowMillis = window.toMillis();
    this.clock = clock;
  }

  public boolean tryAcquire(String key) {
    long now = clock.getAsLong();
    if (windows.size() > SWEEP_THRESHOLD) {
      sweep(now);
    }
    Window window = windows.compute(key, (ignored, current) ->
        current == null || now - current.start >= windowMillis
            ? new Window(now, 1)
            : new Window(current.start, current.count + 1));
    return window.count <= limit;
  }

  /** Segundos até a janela atual da chave terminar (mínimo 1). */
  public long retryAfterSeconds(String key) {
    Window window = windows.get(key);
    if (window == null) {
      return 1;
    }
    long remaining = windowMillis - (clock.getAsLong() - window.start);
    return Math.max(1, (remaining + 999) / 1000);
  }

  private void sweep(long now) {
    Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
    while (iterator.hasNext()) {
      if (now - iterator.next().getValue().start >= windowMillis) {
        iterator.remove();
      }
    }
  }

  private record Window(long start, int count) {
  }
}
