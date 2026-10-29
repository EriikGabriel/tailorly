import type {
  DraftFields,
  ReviewDraft,
  ReviewStepId,
} from "@@types/review-model";
import { CircleCheck, IconExclamationMark } from "@react-zero-ui/icon-sprite";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { createContext, type ReactNode, useContext, useMemo } from "react";
import type { ReviewIssue } from "@/types/review";
import {
  buildVerificationIndex,
  factLocation,
  type VerifiableRecord,
} from "@/utils/review-verification";

/** Identifica uma linha do rascunho: "identity" ou o id `${recordId}-${índice}` da coleção. */
type Scope = string;

export interface PendingFieldsApi {
  /** Pendência do fato que alimenta este campo, se ainda estiver aberta. */
  field: (scope: Scope, key: string) => ReviewIssue | undefined;
  /** Pendências abertas de uma linha inteira. */
  row: (scope: Scope) => ReviewIssue[];
  /** O valor no rascunho já foi alterado em relação ao fato (resolve ao salvar). */
  edited: (issue: ReviewIssue) => boolean;
  /** Confirma o valor atual do fato (resolve a pendência na hora). */
  confirm: (issue: ReviewIssue) => void;
  busy: boolean;
  /** O campo foi confirmado ou corrigido e o rascunho ainda tem esse valor. */
  verified: (scope: Scope, key: string) => boolean;
  /** Todos os fatos da linha foram verificados. */
  rowVerified: (scope: Scope) => boolean;
  /** A etapa tem fatos e nenhuma pendência aberta. */
  stepClear: (step: ReviewStepId) => boolean;
}

const PendingFieldsContext = createContext<PendingFieldsApi | null>(null);

const EMPTY: PendingFieldsApi = {
  field: () => undefined,
  row: () => [],
  edited: () => false,
  confirm: () => undefined,
  busy: false,
  verified: () => false,
  rowVerified: () => false,
  stepClear: () => false,
};

function scopeOf(source: NonNullable<ReviewIssue["source"]>): {
  scope: Scope;
  key: string;
} {
  return factLocation(source);
}

function draftValue(draft: ReviewDraft, scope: Scope, key: string): string {
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

  const api = useMemo<PendingFieldsApi>(() => {
    const byField = new Map<string, ReviewIssue>();
    const byRow = new Map<string, ReviewIssue[]>();
    for (const issue of issues) {
      if (!issue.source) continue;
      const { scope, key } = scopeOf(issue.source);
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
      field: (scope, key) => byField.get(`${scope}|${key}`),
      row: (scope) => byRow.get(scope) ?? [],
      edited: (issue) => {
        if (!issue.source) return false;
        const { scope, key } = scopeOf(issue.source);
        return (
          draftValue(draft, scope, key).trim() !== issue.source.value.trim()
        );
      },
      confirm: onConfirm,
      busy,
    };
  }, [issues, records, clearSteps, draft, onConfirm, busy]);

  return (
    <PendingFieldsContext.Provider value={api}>
      {children}
    </PendingFieldsContext.Provider>
  );
}

export function usePendingFields(): PendingFieldsApi {
  return useContext(PendingFieldsContext) ?? EMPTY;
}

/** Destaque do campo pendente; combine com {@link PendingNote} ao lado do controle. */
export const PENDING_RING = "rounded-xl bg-amber-50/80 p-2";

/**
 * Aviso no próprio campo: por que ele está pendente, o quanto importa para a vaga e como resolver
 * (editar e salvar, limpar e salvar, ou confirmar o valor atual).
 */
export function PendingNote({
  issue,
  inline = false,
}: {
  issue: ReviewIssue;
  inline?: boolean;
}) {
  const pending = usePendingFields();
  const edited = pending.edited(issue);
  const relevance = issue.jobRelevance;

  return (
    <div
      className={`flex flex-wrap items-center gap-1.5 text-[11px] ${inline ? "" : "mt-1"}`}
    >
      <span className="inline-flex items-center gap-1 rounded-full bg-amber-200/80 px-2 py-0.5 font-bold text-amber-900">
        <IconExclamationMark aria-hidden="true" className="size-3 stroke-3" />
        Pendente
      </span>
      {relevance && (
        <span
          className={`rounded-full px-2 py-0.5 font-bold ${relevance.importance === "required" ? "bg-primary-900 text-primary-foreground" : "bg-surface-container text-on-surface"}`}
          title={relevance.label}
        >
          {relevance.importance === "required"
            ? "Obrigatório na vaga"
            : "Desejável na vaga"}
          {" · "}
          {relevance.label}
        </span>
      )}
      {edited ? (
        <span className="text-on-surface-variant">
          Salve a auditoria para registrar a alteração.
        </span>
      ) : (
        <button
          type="button"
          disabled={pending.busy}
          onClick={() => pending.confirm(issue)}
          className="rounded-md border border-outline-variant/60 bg-card px-2 py-0.5 font-semibold text-primary-950 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900 disabled:opacity-50"
        >
          Confirmar valor
        </button>
      )}
    </div>
  );
}

/** Selo de fato verificado (confirmado ou corrigido pela pessoa). */
export function VerifiedMark({
  label = "Verificado",
  showLabel = false,
  className = "",
}: {
  label?: string;
  showLabel?: boolean;
  className?: string;
}) {
  return (
    <span
      role="img"
      aria-label={label}
      title={label}
      className={`inline-flex items-center gap-1 text-green-700 ${className}`}
    >
      <CircleCheck aria-hidden="true" className="size-4 shrink-0" />
      {showLabel && <span className="text-[11px] font-semibold">{label}</span>}
    </span>
  );
}
