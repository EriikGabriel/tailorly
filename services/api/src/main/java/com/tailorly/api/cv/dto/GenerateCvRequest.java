package com.tailorly.api.cv.dto;

import java.util.UUID;

import com.tailorly.api.cv.model.CvPreferences;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * {@code snapshotId} omitido usa o Ground Truth ativo; {@code jobVersionNumber} omitido usa a versão
 * mais recente da vaga; {@code templateId} omitido usa o template atual do owner (a última versão
 * editada ou, sem edição, o embarcado); {@code preferences} omitido usa o padrão (sem diretrizes,
 * otimizando para ATS).
 */
public record GenerateCvRequest(
    @NotNull UUID ownerId,
    @NotNull UUID jobPostingId,
    @Min(1) Integer jobVersionNumber,
    UUID snapshotId,
    UUID templateId,
    @Valid CvPreferences preferences
) {
}
