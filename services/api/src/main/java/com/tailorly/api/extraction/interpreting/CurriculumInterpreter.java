package com.tailorly.api.extraction.interpreting;

import java.util.List;

/**
 * Contrato para transformar texto bruto em propostas revisáveis de fatos.
 *
 * <p>A implementação pode usar regras, um modelo local ou um provedor externo,
 * mas precisa devolver evidência suficiente para a revisão humana. O contrato
 * não persiste nada para evitar sobrescrever decisões existentes.</p>
 */
public interface CurriculumInterpreter {
  List<FactProposal> interpret(List<ExtractedTextPage> pages);
}
