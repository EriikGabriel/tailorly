package com.tailorly.api.extraction.interpreting;

import java.util.List;
import java.util.UUID;

/**
 * Contrato para transformar texto bruto em propostas revisáveis de fatos.
 *
 * <p>A implementação pode usar regras, um modelo local ou um provedor externo — a escolha
 * depende da preferência de IA do {@code ownerId} (veja
 * {@code com.tailorly.api.aipreferences.AiPreferencesService}), com fallback para a
 * configuração global do servidor. O contrato não persiste nada para evitar sobrescrever
 * decisões existentes.</p>
 */
public interface CurriculumInterpreter {
  List<FactProposal> interpret(UUID ownerId, List<ExtractedTextPage> pages);
}
