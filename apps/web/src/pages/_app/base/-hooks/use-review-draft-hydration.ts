import type { BaseReviewQueries } from "@queries/base-review";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { useEffect } from "react";
import { buildDraftFromRecords } from "../-utils/review-draft-hydration";

/**
 * Preenche o rascunho de revisão com os fatos da API quando eles chegam. A store só aceita a
 * hidratação enquanto o rascunho está vazio, então edições da pessoa nunca são sobrescritas.
 */
export function useReviewDraftHydration(
  sourceId: string | null,
  records: BaseReviewQueries["records"],
) {
  const hydrateFromApi = useReviewDraftStore((state) => state.hydrateFromApi);

  useEffect(() => {
    if (!records.isSuccess || !sourceId) return;
    hydrateFromApi(sourceId, buildDraftFromRecords(records.data));
  }, [hydrateFromApi, records.data, records.isSuccess, sourceId]);
}
