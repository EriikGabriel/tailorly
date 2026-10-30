import type {
  DraftFields,
  ReviewDraft,
  ReviewStepId,
} from "@@types/review-model";
import type { ReviewIssue } from "@/types/review";
import { stepForReviewCategory } from "@/utils/review-step-map";
import {
  buildVerificationIndex,
  factLocation,
  type VerifiableRecord,
} from "@/utils/review-verification";
import type { PendingFieldsApi, Scope } from "../-types/pending-fields";

/** Valor padrão fora do provider: nenhum campo pendente nem verificado. */
export const EMPTY_PENDING_FIELDS: PendingFieldsApi = {
  field: () => undefined,
  row: () => [],
  edited: () => false,
  confirm: () => undefined,
  busy: false,
  verified: () => false,
  rowVerified: () => false,
  stepClear: () => false,
  stepIssues: () => [],
};

export function draftValue(
  draft: ReviewDraft,
  scope: Scope,
  key: string,
): string {
  if (scope === "identity") return draft.identity[key] ?? "";
  for (const rows of Object.values(draft)) {
    if (!Array.isArray(rows)) continue;
    const row = (rows as { id: string; fields: DraftFields }[]).find(
      (item) => item.id === scope,
    );
    if (row) return row.fields[key] ?? "";
  }
  return "";
}

type PendingFieldsInput = {
  issues: readonly ReviewIssue[];
  records: readonly VerifiableRecord[];
  clearSteps: Record<ReviewStepId, boolean>;
  draft: ReviewDraft;
  onConfirm: (issue: ReviewIssue) => void;
  busy: boolean;
};

/** Indexa as pendências por campo, linha e etapa e monta a API consultada pelos formulários. */
export function createPendingFieldsApi({
  issues,
  records,
  clearSteps,
  draft,
  onConfirm,
  busy,
}: PendingFieldsInput): PendingFieldsApi {
  const byField = new Map<string, ReviewIssue>();
  const byRow = new Map<string, ReviewIssue[]>();
  const byStep = new Map<ReviewStepId, ReviewIssue[]>();
  for (const issue of issues) {
    const step = stepForReviewCategory(issue.category);
    if (step) byStep.set(step, [...(byStep.get(step) ?? []), issue]);
    if (!issue.source) continue;
    const { scope, key } = factLocation(issue.source);
    byField.set(`${scope}|${key}`, issue);
    byRow.set(scope, [...(byRow.get(scope) ?? []), issue]);
  }
  const verification = buildVerificationIndex(records);
  return {
    verified: (scope, key) => {
      const value = verification.fields.get(`${scope}|${key}`);
      // Alterar o campo depois de verificado tira a marca até salvar de novo.
      return (
        value !== undefined &&
        !byField.has(`${scope}|${key}`) &&
        draftValue(draft, scope, key).trim() === value.trim()
      );
    },
    rowVerified: (scope) => verification.rows.has(scope) && !byRow.has(scope),
    stepClear: (step) => clearSteps[step] ?? false,
    stepIssues: (step) => byStep.get(step) ?? [],
    field: (scope, key) => byField.get(`${scope}|${key}`),
    row: (scope) => byRow.get(scope) ?? [],
    edited: (issue) => {
      if (!issue.source) return false;
      const { scope, key } = factLocation(issue.source);
      return draftValue(draft, scope, key).trim() !== issue.source.value.trim();
    },
    confirm: onConfirm,
    busy,
  };
}
