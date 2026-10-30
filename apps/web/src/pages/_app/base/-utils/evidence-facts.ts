import type { FactRecord } from "@@types/base-review";
import type { ExtractedPageResponse } from "@lib/api/generated/model/extractedPageResponse";
import { formatFactValue } from "@/utils/fact-display";
import type { EvidenceFact } from "../-types/evidence";
import { parseFactFieldKey } from "./fact-field-key";

/** Fatos confirmados com trecho de evidência e página resolvida, prontos para marcar no PDF. */
export function buildEvidenceFacts(
  records: FactRecord[],
  pages: ExtractedPageResponse[],
): EvidenceFact[] {
  // `pageNumber` é opcional na API; na ausência dele, usamos a ordem de
  // retorno das páginas como aproximação (a maioria dos currículos tem 1-2
  // páginas, então isso raramente diverge da numeração real).
  const pageNumberById = new Map(
    pages.map((page, index) => [page.id, page.pageNumber ?? index + 1]),
  );
  const facts: EvidenceFact[] = [];

  for (const record of records) {
    for (const fact of record.facts) {
      if (fact.reviewState === "omitido") continue;
      const evidence = fact.evidence[0];
      if (!evidence?.sourceExcerpt) continue;
      const pageNumber = pageNumberById.get(evidence.extractedPageId);
      if (!pageNumber) continue;

      facts.push({
        id: fact.id,
        category: record.category,
        value: formatFactValue(fact.reviewedValue ?? fact.extractedValue),
        excerpt: evidence.sourceExcerpt,
        pageNumber,
        recordKey: recordKeyFor(record, fact.fieldKey),
      });
    }
  }

  return facts;
}

// A identidade é plana e não tem índice. O formato do id é o mesmo usado em
// `useReviewDraftStore` ao hidratar o rascunho, para que `expandedRecords`
// aponte exatamente para este registro.
function recordKeyFor(record: FactRecord, fieldKey: string): string | null {
  if (record.category === "identity") return null;
  return `${record.id}-${parseFactFieldKey(fieldKey).index}`;
}

/** Agrupa os fatos pela página do PDF em que a evidência foi encontrada. */
export function groupFactsByPage(
  facts: EvidenceFact[],
): Map<number, EvidenceFact[]> {
  const byPage = new Map<number, EvidenceFact[]>();
  for (const fact of facts) {
    const list = byPage.get(fact.pageNumber) ?? [];
    list.push(fact);
    byPage.set(fact.pageNumber, list);
  }
  return byPage;
}
