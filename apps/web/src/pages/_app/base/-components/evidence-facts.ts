import type { ExtractedPageResponse } from "@lib/api/generated/model/extractedPageResponse";
import type { FactRecord } from "@queries/base-review";
import { formatFactValue } from "@/utils/fact-display";

export type EvidenceFact = {
  id: string;
  category: string;
  value: string;
  excerpt: string;
  pageNumber: number;
  /**
   * Identifica a entrada repetível (ex.: "Experiência 2") a que o fato
   * pertence, no mesmo formato usado por `expandedRecords` do rascunho de
   * revisão (`${recordId}-${index}`). `null` para categorias sem repetição
   * (ex.: identidade), que não têm um registro específico para focar.
   */
  recordKey: string | null;
};

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

// Fatos de categorias repetíveis usam chaves "{index}:campo" (ex.: "0:role");
// a identidade é plana e não tem índice. Mantém o mesmo formato de id usado
// em `useReviewDraftStore` ao hidratar o rascunho, para que `expandedRecords`
// aponte exatamente para este registro.
function recordKeyFor(record: FactRecord, fieldKey: string): string | null {
  if (record.category === "identity") return null;
  const match = /^(\d+):(.*)$/.exec(fieldKey);
  const index = match ? Number(match[1]) : 0;
  return `${record.id}-${index}`;
}
