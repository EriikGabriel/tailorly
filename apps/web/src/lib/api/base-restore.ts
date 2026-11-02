import { type ApiRequestOptions, apiBlob, apiRequest } from "./client";
import type { BaseVersionResponse } from "./generated/model";

// Rotas usadas para retomar o Currículo Base depois de recarregar a página. Escritas à mão porque ainda
// não foram exportadas para `services/api/openapi/tailorly.json`; `pnpm run api:sync` as gera.

/** Versões do owner, da mais nova para a mais antiga. */
export const baseVersionFindByOwner = (
  ownerId: string,
  options?: ApiRequestOptions,
) =>
  apiRequest<BaseVersionResponse[]>(
    `/v1/base-versions/owner/${encodeURIComponent(ownerId)}`,
    options,
  );

/** PDF original guardado no servidor. */
export const baseDocumentFile = (id: string, options?: ApiRequestOptions) =>
  apiBlob(`/v1/base-documents/${encodeURIComponent(id)}/file`, {
    accept: "application/pdf",
    ...options,
  });
