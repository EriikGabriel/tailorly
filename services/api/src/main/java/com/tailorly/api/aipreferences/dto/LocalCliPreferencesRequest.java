package com.tailorly.api.aipreferences.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Escolhas da conta para um provider local por CLI; nulos usam o padrão do servidor. O modelo vira
 * argumento do CLI, por isso só aceita nomes simples (sem espaço nem hífen inicial); vazio usa o padrão.
 * {@code efforts}: chaves {@code default} e etapas de {@code LocalCliEfforts.TASKS}, níveis
 * {@code low}..{@code max} (validados no serviço).
 */
public record LocalCliPreferencesRequest(
    @Size(max = 255)
    @Pattern(regexp = "^$|[A-Za-z0-9][A-Za-z0-9._:/\\[\\]-]*")
    String model,

    @Min(10)
    @Max(1800)
    Integer timeoutSeconds,

    Map<String, String> efforts
) {
}
