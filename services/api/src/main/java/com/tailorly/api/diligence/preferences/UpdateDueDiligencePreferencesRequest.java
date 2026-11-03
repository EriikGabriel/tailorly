package com.tailorly.api.diligence.preferences;

/** Atualização parcial: só o campo enviado muda. */
public record UpdateDueDiligencePreferencesRequest(Boolean pipelineEnabled) {
}
