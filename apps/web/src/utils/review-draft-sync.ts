import type { DraftCollection, ReviewDraft } from "../types/review-model";

const COLLECTIONS: readonly string[] = [
  "experience",
  "volunteering",
  "projects",
  "degrees",
  "certifications",
  "languages",
  "technologies",
];

export interface AnsweredFact {
  /** Categoria do registro do fato (ex.: "identity", "experience"). */
  category: string;
  recordId: string;
  fieldKey: string;
}

function changeField(
  fields: Record<string, string>,
  key: string,
  result: "confirmed" | "corrected" | "omitted",
  correction: string,
): Record<string, string> {
  if (result === "corrected") return { ...fields, [key]: correction };
  if (result === "omitted") {
    const { [key]: _removed, ...rest } = fields;
    return rest;
  }
  return fields;
}

/**
 * Reflete no rascunho da auditoria uma pendência já respondida na API. Sem isso, o rascunho
 * continuaria com o valor antigo e o próximo "Salvar" sobrescreveria, em silêncio, a correção
 * (ou a omissão) que a pessoa acabou de fazer. Confirmar não muda o valor.
 */
export function applyAnswerToDraft(
  draft: ReviewDraft,
  fact: AnsweredFact,
  result: "confirmed" | "corrected" | "omitted",
  correction: string,
): ReviewDraft {
  if (result === "confirmed") return draft;

  if (fact.category === "identity") {
    return {
      ...draft,
      identity: changeField(draft.identity, fact.fieldKey, result, correction),
    };
  }
  if (!COLLECTIONS.includes(fact.category)) return draft;

  const match = /^(\d+):(.*)$/.exec(fact.fieldKey);
  const index = match ? Number(match[1]) : 0;
  const key = match ? match[2] : fact.fieldKey;
  const collection = fact.category as DraftCollection;
  const rowId = `${fact.recordId}-${index}`;

  let touched = false;
  const rows = draft[collection].map((row) => {
    if (row.id !== rowId) return row;
    touched = true;
    return { ...row, fields: changeField(row.fields, key, result, correction) };
  });
  return touched ? { ...draft, [collection]: rows } : draft;
}
