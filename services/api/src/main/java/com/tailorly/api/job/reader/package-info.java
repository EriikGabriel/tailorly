/**
 * Leitura de uma vaga a partir de um link ou de texto colado: baixa a página com Jsoup (somente
 * URLs permitidas por {@code JobUrlPolicy}) e extrai só o que ela declara, sem inferir nada.
 *
 * <p>Os extratores seguem a ordem de prioridade Gupy, JSON-LD {@code JobPosting} e HTML
 * genérico, cada um completando o que o anterior não trouxe; texto colado é sempre tratado como
 * texto puro. {@code JobPostingExtractionService} combina o resultado e calcula o hash de
 * versão da vaga.</p>
 */
package com.tailorly.api.job.reader;
