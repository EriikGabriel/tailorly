package com.tailorly.api.aipreferences.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * {@code remoteApiKey}/{@code ollamaApiKey} nulos mantêm a chave já guardada (o frontend nunca
 * recebe o valor em texto puro de volta, então não tem como reenviá-lo); uma string, mesmo
 * vazia, substitui o valor guardado.
 */
public record UpdateAiPreferencesRequest(
    @NotBlank
    @Pattern(regexp = "rules|remote|remote-with-ollama-fallback|ollama-only")
    String provider,

    String remoteEndpoint,
    String remoteModel,
    String remoteApiKey,
    String ollamaEndpoint,
    String ollamaModel,
    String ollamaApiKey
) {
}
