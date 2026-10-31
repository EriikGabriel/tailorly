package com.tailorly.api.vault.dto;

import jakarta.validation.constraints.Size;

/**
 * Preferências de segundo cérebro da conta. {@code relativePath} vazio remove o vault; leitura e
 * exportação exigem um vault. {@code exportSubfolder} vazia usa o padrão do servidor.
 */
public record UpdateOwnerVaultRequest(
    @Size(max = 1024) String relativePath,
    boolean readEnabled,
    boolean exportEnabled,
    @Size(max = 512) String exportSubfolder,
    boolean webSearchEnabled
) {
}
