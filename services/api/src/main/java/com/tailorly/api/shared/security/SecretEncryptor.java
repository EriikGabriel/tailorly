package com.tailorly.api.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Criptografa segredos de conta em repouso (hoje, chaves de API de provedores de IA em
 * {@link com.tailorly.api.aipreferences.AiPreferences}) com AES-GCM via
 * {@code TAILORLY_SECRETS_KEY}.
 *
 * <p>O salt usado pelo {@link TextEncryptor} é derivado deterministicamente da própria chave
 * (hash SHA-256) para não exigir um segundo segredo gerenciado separadamente; a chave em si
 * precisa ter entropia suficiente, assim como {@code TAILORLY_JWT_SECRET}.</p>
 */
@Component
public class SecretEncryptor {
  private final TextEncryptor encryptor;

  public SecretEncryptor(@Value("${tailorly.security.secrets-key}") String secretsKey) {
    this.encryptor = Encryptors.delux(secretsKey, deriveSalt(secretsKey));
  }

  public String encrypt(String plainText) {
    return plainText == null || plainText.isBlank() ? null : encryptor.encrypt(plainText);
  }

  public String decrypt(String cipherText) {
    return cipherText == null ? null : encryptor.decrypt(cipherText);
  }

  private static String deriveSalt(String secretsKey) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(secretsKey.getBytes(StandardCharsets.UTF_8));
      return new String(Hex.encode(hash)).substring(0, 16);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
