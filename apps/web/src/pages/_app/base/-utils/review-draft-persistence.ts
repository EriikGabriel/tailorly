import type { ApiReviewIssue, FactRecord } from "@@types/base-review";
import { draftCollections, type ReviewDraft } from "@@types/review-model";
import { indexedFieldKey } from "./fact-field-key";

/** Campos de uma categoria prontos para gravar: `fieldKey` -> valor. */
export type FactGroup = { category: string; fields: Record<string, string> };

/**
 * Converte o rascunho em grupos de fatos por categoria. Uma categoria vazia só entra se já existe
 * registro no servidor, para que apagar tudo nela também seja gravado (omitindo os fatos).
 */
export function buildFactGroups(
  draft: ReviewDraft,
  records: FactRecord[],
): FactGroup[] {
  const hasRecord = (category: string) =>
    records.some((item) => item.category === category);
  const groups: FactGroup[] = [];

  if (Object.values(draft.identity).some(Boolean) || hasRecord("identity")) {
    groups.push({ category: "identity", fields: draft.identity });
  }

  for (const category of draftCollections) {
    const fields: Record<string, string> = {};
    draft[category].forEach((entry, index) => {
      for (const [key, value] of Object.entries(entry.fields)) {
        if (value.trim()) fields[indexedFieldKey(index, key)] = value.trim();
      }
    });
    if (Object.keys(fields).length || hasRecord(category)) {
      groups.push({ category, fields });
    }
  }

  return groups;
}

/**
 * Pendências abertas por fato. Editar ou remover um campo pendente resolve a pendência (a
 * resposta registra a decisão); só atualizar o fato deixaria a pendência aberta e a aprovação
 * bloqueada.
 */
export function openIssueIdByFact(
  issues: ApiReviewIssue[],
): Map<string, string> {
  return new Map(
    issues
      .filter((issue) => issue.status === "PENDING" && issue.factId)
      .map((issue) => [issue.factId as string, issue.id]),
  );
}

/** Há alguma pendência bloqueante em aberto? Ela impede a aprovação do Ground Truth. */
export function hasBlockingIssues(issues: ApiReviewIssue[]): boolean {
  return issues.some((issue) => issue.blocking && issue.status === "PENDING");
}

export function countBlockingIssues(issues: ApiReviewIssue[]): number {
  return issues.filter((issue) => issue.blocking && issue.status === "PENDING")
    .length;
}
