package com.tailorly.api.session;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites e TTL do acesso anônimo ({@code tailorly.anonymous.*}). Todos são aplicados no
 * servidor; nada aqui depende de estado do navegador.
 */
@ConfigurationProperties(prefix = "tailorly.anonymous")
public record AnonymousProperties(
    Duration sessionTtl,
    long maxFileBytes,
    int maxPages,
    int maxUploads,
    int maxExtractions,
    int maxGenerations,
    int requestsPerMinute,
    int sessionsPerIpPerHour,
    int maxActiveSessions,
    Duration cleanupInterval,
    boolean cleanupEnabled
) {
}
