package com.tailorly.api.shared.security;

import java.security.SecureRandom;
import java.util.Base64;

import com.tailorly.api.shared.util.Sha256;

/**
 * Tokens opacos de refresh, no mesmo formato de {@link com.tailorly.api.session.AnonymousSessionTokens}:
 * 256 bits aleatórios com prefixo fixo. O servidor guarda só o SHA-256 do token.
 */
public final class UserSessionTokens {
  public static final String PREFIX = "tlr_rt_";

  private static final SecureRandom RANDOM = new SecureRandom();

  private UserSessionTokens() {
  }

  public static String generate() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String token) {
    return Sha256.hex(token);
  }
}
