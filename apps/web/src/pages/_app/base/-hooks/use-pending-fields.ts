import { createContext, useContext } from "react";
import type { PendingFieldsApi } from "../-types/pending-fields";
import { EMPTY_PENDING_FIELDS } from "../-utils/pending-fields";

export const PendingFieldsContext = createContext<PendingFieldsApi | null>(
  null,
);

/** Pendências e verificações dos campos da revisão; fora do provider, nada está pendente. */
export function usePendingFields(): PendingFieldsApi {
  return useContext(PendingFieldsContext) ?? EMPTY_PENDING_FIELDS;
}
