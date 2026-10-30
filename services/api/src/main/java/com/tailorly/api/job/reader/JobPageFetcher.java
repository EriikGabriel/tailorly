package com.tailorly.api.job.reader;

public interface JobPageFetcher {
  /** Baixa a página de {@code url}; lança exceções de domínio quando o link não é permitido ou falha. */
  FetchedPage fetch(String url);
}
