package com.tailorly.api.objects.storage;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3StorageConfiguration {
  @Bean(destroyMethod = "close")
  S3Client s3Client(
      @Value("${tailorly.storage.endpoint}") String endpoint,
      @Value("${tailorly.storage.region}") String region,
      @Value("${tailorly.storage.access-key}") String accessKey,
      @Value("${tailorly.storage.secret-key}") String secretKey,
      @Value("${tailorly.storage.path-style}") boolean pathStyle
  ) {
    if (accessKey.isBlank() != secretKey.isBlank()) {
      throw new IllegalStateException("Both storage access key and secret key must be configured");
    }
    var builder = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyle)
            .chunkedEncodingEnabled(false)
            .build());
    if (!accessKey.isBlank()) {
      builder.credentialsProvider(StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey)));
    }
    return builder.build();
  }
}
