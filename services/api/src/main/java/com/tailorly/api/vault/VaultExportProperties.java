package com.tailorly.api.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Exportação opcional dos currículos gerados para uma pasta local ({@code tailorly.vault.*}),
 * desligada por padrão. {@code path} é o diretório raiz (por exemplo o vault Obsidian de uma
 * pessoa) e {@code subfolder} o caminho dentro dele onde os artefatos são copiados.
 */
@ConfigurationProperties(prefix = "tailorly.vault")
public record VaultExportProperties(
    boolean enabled,
    String path,
    String subfolder
) {
}
