package com.tailorly.api.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {
  private final AtomicLong now = new AtomicLong(1_000_000);
  private final FixedWindowRateLimiter limiter =
      new FixedWindowRateLimiter(2, Duration.ofMinutes(1), now::get);

  @Test
  void allowsUpToTheLimitThenBlocksWithinTheWindow() {
    assertTrue(limiter.tryAcquire("a"));
    assertTrue(limiter.tryAcquire("a"));
    assertFalse(limiter.tryAcquire("a"));
  }

  @Test
  void keysAreIndependent() {
    limiter.tryAcquire("a");
    limiter.tryAcquire("a");
    assertTrue(limiter.tryAcquire("b"));
  }

  @Test
  void windowResetsAfterItElapses() {
    limiter.tryAcquire("a");
    limiter.tryAcquire("a");
    assertFalse(limiter.tryAcquire("a"));

    now.addAndGet(Duration.ofMinutes(1).toMillis());

    assertTrue(limiter.tryAcquire("a"));
  }

  @Test
  void retryAfterReportsRemainingSecondsOfTheWindow() {
    limiter.tryAcquire("a");
    now.addAndGet(20_000);

    assertEquals(40, limiter.retryAfterSeconds("a"));
  }
}
