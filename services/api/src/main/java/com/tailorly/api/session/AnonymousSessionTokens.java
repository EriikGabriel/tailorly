package com.tailorly.api.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos de sessão anônima: 256 bits aleatórios com prefixo fixo. O servidor guarda só o
 * SHA-256 do token, então um vazamento do banco não permite assumir uma sessão.
 */
public final class AnonymousSessionTokens {
  public static final String PREFIX = "tlr_anon_";

  private static final SecureRandom RANDOM = new SecureRandom();

  private AnonymousSessionTokens() {
  }

  public static String generate() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static boolean isAnonymousToken(String token) {
    return token != null && token.startsWith(PREFIX);
  }

  public static String hash(String token) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
