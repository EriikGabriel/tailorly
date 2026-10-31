package com.tailorly.api.search;

import java.util.List;

/**
 * Busca web para encontrar páginas sobre um projeto. A implementação fica atrás desta interface
 * para que o provedor possa ser trocado; sem configuração, {@link #isConfigured()} é falso e nenhuma
 * busca é feita.
 */
public interface WebSearchClient {
  boolean isConfigured();

  /** Resultados em ordem de relevância; cada URL ainda precisa passar pela política de acesso a páginas. */
  List<SearchResult> search(String query, int count);

  record SearchResult(String url, String title) {
  }
}
