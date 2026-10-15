package com.tailorly.api.objects.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3ObjectStorage implements ObjectStorage {
  private final S3Client client;
  private final String bucket;

  public S3ObjectStorage(S3Client client, @Value("${tailorly.storage.bucket}") String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public void put(String key, byte[] content, String mimeType) {
    client.putObject(PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(mimeType)
        .build(), RequestBody.fromBytes(content));
  }

  @Override
  public boolean exists(String key) {
    try {
      client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return true;
    } catch (NoSuchKeyException ex) {
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }
      throw ex;
    }
  }

  @Override
  public void delete(String key) {
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }
}
