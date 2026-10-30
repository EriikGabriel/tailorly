package com.tailorly.api.shared.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 em hexadecimal minúsculo, usado para impressões digitais de conteúdo (arquivos, snapshots,
 * templates, vagas) e para guardar tokens sem o valor original.
 */
public final class Sha256 {

  private Sha256() {
  }

  public static String hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      // Todo JRE é obrigado a oferecer SHA-256; chegar aqui indica um ambiente quebrado.
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  /** Hash do texto codificado em UTF-8. */
  public static String hex(String text) {
    return hex(text.getBytes(StandardCharsets.UTF_8));
  }
}
