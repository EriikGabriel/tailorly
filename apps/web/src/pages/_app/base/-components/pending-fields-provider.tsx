import type { ReviewStepId } from "@@types/review-model";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { type ReactNode, useMemo } from "react";
import type { ReviewIssue } from "@/types/review";
import type { VerifiableRecord } from "@/utils/review-verification";
import { PendingFieldsContext } from "../-hooks/use-pending-fields";
import { createPendingFieldsApi } from "../-utils/pending-fields";

/** Disponibiliza para os campos da revisão quais estão pendentes ou verificados. */
export function PendingFieldsProvider({
  issues,
  records,
  clearSteps,
  onConfirm,
  busy,
  children,
}: {
  issues: readonly ReviewIssue[];
  records: readonly VerifiableRecord[];
  clearSteps: Record<ReviewStepId, boolean>;
  onConfirm: (issue: ReviewIssue) => void;
  busy: boolean;
  children: ReactNode;
}) {
  const draft = useReviewDraftStore((state) => state.draft);

  const api = useMemo(
    () =>
      createPendingFieldsApi({
        issues,
        records,
        clearSteps,
        draft,
        onConfirm,
        busy,
      }),
    [issues, records, clearSteps, draft, onConfirm, busy],
  );

  return (
    <PendingFieldsContext.Provider value={api}>
      {children}
    </PendingFieldsContext.Provider>
  );
}
