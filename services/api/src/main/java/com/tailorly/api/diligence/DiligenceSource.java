package com.tailorly.api.diligence;

/** Fonte pública candidata (resultado de busca) numerada no prompt; o trecho citável é título + resumo do buscador. */
record DiligenceSource(int id, String url, String title, String snippet) {
}
