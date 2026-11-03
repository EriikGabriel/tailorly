package com.tailorly.api.cv.defaults.dto;

import java.util.List;

import com.tailorly.api.cv.model.CvLanguage;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial dos padrões da conta: só os campos enviados mudam. {@code templateName} é o
 * nome de um template embarcado (ex.: {@code classic}, {@code harvard}); {@code guidelines} substitui a lista
 * inteira (vazia desliga todas); {@code language} é {@code auto}, {@code pt-BR} ou {@code en}.
 */
public record UpdateGenerationDefaultsRequest(
    @Size(min = 1, max = 64) String templateName,
    Boolean optimizeForAts,
    @Size(max = 10) List<@Size(min = 1, max = 80) String> guidelines,
    @Pattern(regexp = CvLanguage.PREFERENCE_PATTERN) String language
) {
}
