import type { ReviewStepId } from "@@types/review-model";
import type { ReviewIssue } from "@/types/review";

/** Identifica uma linha do rascunho: "identity" ou o id `${recordId}-${índice}` da coleção. */
export type Scope = string;

export interface PendingFieldsApi {
  /** Pendência do fato que alimenta este campo, se ainda estiver aberta. */
  field: (scope: Scope, key: string) => ReviewIssue | undefined;
  /** Pendências abertas de uma linha inteira. */
  row: (scope: Scope) => ReviewIssue[];
  /** O valor no rascunho já foi alterado em relação ao fato (resolve ao salvar). */
  edited: (issue: ReviewIssue) => boolean;
  /** Confirma o valor atual do fato (resolve a pendência na hora). */
  confirm: (issue: ReviewIssue) => void | Promise<void>;
  busy: boolean;
  /** O campo foi confirmado ou corrigido e o rascunho ainda tem esse valor. */
  verified: (scope: Scope, key: string) => boolean;
  /** Todos os fatos da linha foram verificados. */
  rowVerified: (scope: Scope) => boolean;
  /** A etapa tem fatos e nenhuma pendência aberta. */
  stepClear: (step: ReviewStepId) => boolean;
  /** Pendências abertas de uma etapa inteira. */
  stepIssues: (step: ReviewStepId) => ReviewIssue[];
}
