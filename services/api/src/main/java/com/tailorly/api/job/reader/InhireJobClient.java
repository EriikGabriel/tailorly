package com.tailorly.api.job.reader;

import java.util.Optional;

import tools.jackson.databind.JsonNode;

/** Acesso à API pública de páginas de carreira da InHire. */
public interface InhireJobClient {
  /** Dados da vaga publicada, ou vazio quando ela não existe mais. Falhas de rede viram exceção de domínio. */
  Optional<JsonNode> fetch(String tenant, String jobId);
}
