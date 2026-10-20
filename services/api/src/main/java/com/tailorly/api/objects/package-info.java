/**
 * Metadados de arquivos gravados no storage privado (interface {@code ObjectStorage},
 * implementado hoje sobre S3/Garage; veja {@link com.tailorly.api.objects.storage}).
 *
 * <p>Um {@link com.tailorly.api.objects.StoredObject} nunca é criado isoladamente por uma rota
 * pública que grave bytes: o upload do Currículo Base
 * ({@code com.tailorly.api.base.document.BaseDocumentUploadService}) e o worker de extração
 * ({@code com.tailorly.api.extraction.ExtractionWorker}) gravam o arquivo primeiro e só depois
 * registram o metadado. {@code POST /v1/stored-objects} e a alteração de
 * {@code deletionStatus} mexem apenas em metadados, nunca no conteúdo do arquivo.</p>
 */
package com.tailorly.api.objects;
