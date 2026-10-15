package com.tailorly.api.objects.storage;

public interface ObjectStorage {
  void put(String key, byte[] content, String mimeType);

  boolean exists(String key);

  void delete(String key);
}
