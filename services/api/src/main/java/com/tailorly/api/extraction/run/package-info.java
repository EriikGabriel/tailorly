/**
 * Execuções do pipeline de extração ({@code ExtractionRun}): fila, fingerprint do pipeline,
 * status ({@code QUEUED}/{@code PROCESSING}/{@code COMPLETED}/{@code FAILED}) e metadados de
 * tempo/erro. O ciclo de vida é controlado por
 * {@code com.tailorly.api.extraction.ExtractionQueue}; este pacote expõe apenas a entidade, o
 * repositório e as consultas HTTP.
 */
package com.tailorly.api.extraction.run;
