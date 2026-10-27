package com.tailorly.api.objects.storage;

public interface ObjectStorage {
  byte[] get(String key);

  void put(String key, byte[] content, String mimeType);

  boolean exists(String key);

  void delete(String key);

  /** Remove todos os objetos cuja chave começa com o prefixo; usado na limpeza de dados temporários. */
  void deleteByPrefix(String prefix);
}
