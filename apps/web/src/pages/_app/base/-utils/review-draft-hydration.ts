import type { FactRecord } from "@@types/base-review";
import {
  createReviewDraft,
  type DraftCollection,
  draftCollections,
  type ReviewDraft,
} from "@@types/review-model";
import { parseFactFieldKey } from "./fact-field-key";

const factText = (fact: FactRecord["facts"][number]) =>
  String(fact.reviewedValue ?? fact.extractedValue ?? "");

function isDraftCollection(category: string): category is DraftCollection {
  return (draftCollections as readonly string[]).includes(category);
}

/**
 * Monta o rascunho editável a partir dos fatos da API. Fatos omitidos ficam de fora; o `id` de
 * cada entrada (`${recordId}-${index}`) é o mesmo usado para focar o registro no documento.
 */
export function buildDraftFromRecords(records: FactRecord[]): ReviewDraft {
  const draft = createReviewDraft();

  for (const record of records) {
    if (record.category === "identity") {
      for (const fact of record.facts) {
        if (fact.reviewState !== "omitido") {
          draft.identity[fact.fieldKey] = factText(fact);
        }
      }
      continue;
    }

    if (!isDraftCollection(record.category)) continue;

    const rows = new Map<number, Record<string, string>>();
    for (const fact of record.facts) {
      if (fact.reviewState === "omitido") continue;
      const { index, key } = parseFactFieldKey(fact.fieldKey);
      const fields = rows.get(index) ?? {};
      fields[key] = factText(fact);
      rows.set(index, fields);
    }

    draft[record.category] = [...rows.entries()]
      .sort(([left], [right]) => left - right)
      .map(([index, fields]) => ({ id: `${record.id}-${index}`, fields }));
  }

  return draft;
}
