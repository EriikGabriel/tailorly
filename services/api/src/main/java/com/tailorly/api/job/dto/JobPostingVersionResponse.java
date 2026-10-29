package com.tailorly.api.job.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma versão gravada da vaga. {@code reused} é verdadeiro quando o conteúdo lido era idêntico ao da
 * versão mais recente e nenhuma versão nova foi criada. {@code analysisStatus}: {@code completed},
 * {@code unavailable} (sem IA configurada), {@code failed} ou {@code quota_exceeded}.
 */
public record JobPostingVersionResponse(
    UUID id,
    UUID jobPostingId,
    int versionNumber,
    String contentHash,
    JobPostingResponse details,
    JobAnalysis analysis,
    String analysisStatus,
    Instant analyzedAt,
    Instant createdAt,
    Instant lastCheckedAt,
    boolean reused
) {
}
