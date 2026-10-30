/**
 * Interpretação semântica do Currículo Base: transforma o texto extraído de cada página em
 * propostas de fatos, sempre com evidência verificável na página de origem.
 *
 * <p>{@link com.tailorly.api.extraction.interpreting.RoutingCurriculumInterpreter} escolhe a
 * estratégia pelo {@code provider} configurado (IA remota, Ollama local, remota com fallback ou
 * regras). A estratégia de IA é dividida em prompt, esquema de fatos, localização do trecho e
 * validação da resposta. A persistência das propostas fica em
 * {@code base.fact.FactProposalPersistenceService}, que preserva fatos confirmados, corrigidos
 * ou omitidos pela pessoa.</p>
 */
package com.tailorly.api.extraction.interpreting;
