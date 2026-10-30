/**
 * Vagas por owner (conta ou sessão anônima): leitura por link ou texto, gravação em versões
 * imutáveis e análise por IA.
 *
 * <p>Este pacote tem a API HTTP ({@code JobPostingController}) e a orquestração
 * ({@code JobPostingService}). A leitura da página fica em {@code job.reader} (download,
 * política de URL e extratores por fonte); a análise de requisitos por IA fica em
 * {@code job.analysis}; os DTOs em {@code job.dto} e as entidades em {@code job.persistence}.</p>
 */
package com.tailorly.api.job;
