package com.tailorly.api.vault;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra {@link VaultExportService} só quando {@code tailorly.vault.enabled=true} (desligado
 * por padrão, ao contrário dos jobs de limpeza que usam {@code matchIfMissing = true}) — ver
 * {@code com.tailorly.api.user.retention.AccountRetentionConfiguration} para o mesmo padrão.
 */
@Configuration
@EnableConfigurationProperties(VaultExportProperties.class)
public class VaultExportConfiguration {

  @Bean
  @ConditionalOnProperty(name = "tailorly.vault.enabled", havingValue = "true", matchIfMissing = false)
  public VaultExportService vaultExportService() {
    return new VaultExportService();
  }
}
