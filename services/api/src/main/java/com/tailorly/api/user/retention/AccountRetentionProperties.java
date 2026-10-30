package com.tailorly.api.user.retention;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política de retenção de dados para contas desativadas ({@code tailorly.account.*}). Depois de
 * {@code retentionPeriod} desde {@code users.disabled_at}, o job de retenção apaga o currículo
 * base, os snapshots, as vagas e os currículos gerados do owner da conta — a linha de
 * {@code users}/{@code owners} continua existindo, desativada.
 */
@ConfigurationProperties(prefix = "tailorly.account")
public record AccountRetentionProperties(
    Duration retentionPeriod,
    Duration cleanupInterval,
    boolean cleanupEnabled
) {
}
