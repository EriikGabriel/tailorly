package com.tailorly.api.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Exportação opcional dos currículos gerados para o vault da conta ({@code tailorly.vault.*}),
 * desligada por padrão. {@code subfolder} é o caminho dentro do vault onde os artefatos são copiados;
 * o vault em si vem de {@link OwnerVaultService}.
 */
@ConfigurationProperties(prefix = "tailorly.vault")
public record VaultExportProperties(
    boolean enabled,
    String subfolder
) {
}
