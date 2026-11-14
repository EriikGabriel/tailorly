package com.tailorly.api.aipreferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * {@code remoteApiKey}/{@code ollamaApiKey} nulos mantêm a chave já guardada (o frontend nunca
 * recebe o valor em texto puro de volta, então não tem como reenviá-lo); uma string, mesmo
 * vazia, substitui o valor guardado.
 *
 * <p>{@code claudeCli}/{@code codexCli} só valem para os providers {@code claude-cli}/{@code codex-cli}
 * (uso local); um bloco nulo mantém o que já estava salvo para aquele CLI (ver
 * {@link LocalCliPreferencesRequest}).
 */
public record UpdateAiPreferencesRequest(
    @NotBlank
    @Pattern(regexp = "rules|remote|remote-with-ollama-fallback|ollama-only|claude-cli|codex-cli")
    String provider,

    String remoteEndpoint,
    String remoteModel,
    String remoteApiKey,
    String ollamaEndpoint,
    String ollamaModel,
    String ollamaApiKey,

    @Valid
    LocalCliPreferencesRequest claudeCli,

    @Valid
    LocalCliPreferencesRequest codexCli
) {
}
