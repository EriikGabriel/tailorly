/** Só o que a verificação lê; `FactRecord` e `BaseFact` da revisão satisfazem estes formatos. */
export interface VerifiableFact {
  recordId: string;
  fieldKey: string;
  extractedValue: unknown;
  reviewedValue: unknown;
  reviewState: string;
}

export interface VerifiableRecord {
  id: string;
  category: string;
  facts: VerifiableFact[];
}

/**
 * Localiza o fato no rascunho da auditoria: "identity" ou a linha `${recordId}-${índice}` de uma
 * coleção, mais a chave do campo dentro dela.
 */
export function factLocation(fact: {
  category: string;
  recordId: string;
  fieldKey: string;
}): { scope: string; key: string } {
  if (fact.category === "identity")
    return { scope: "identity", key: fact.fieldKey };
  const match = /^(\d+):(.*)$/.exec(fact.fieldKey);
  return {
    scope: `${fact.recordId}-${match ? match[1] : "0"}`,
    key: match ? match[2] : fact.fieldKey,
  };
}

/** Valor do fato no mesmo formato do rascunho. */
export function factValue(fact: VerifiableFact): string {
  return String(fact.reviewedValue ?? fact.extractedValue ?? "");
}

/** Confirmado ou corrigido pela pessoa: o fato foi verificado; pendente e omitido, não. */
export function isVerifiedState(state: string): boolean {
  return state === "confirmado" || state === "corrigido";
}

export interface VerificationIndex {
  /** Campos verificados: `${scope}|${key}` -> valor verificado. */
  fields: ReadonlyMap<string, string>;
  /** Linhas em que todos os fatos (exceto omitidos) estão verificados. */
  rows: ReadonlySet<string>;
}

export function buildVerificationIndex(
  records: readonly VerifiableRecord[],
): VerificationIndex {
  const fields = new Map<string, string>();
  const unverified = new Set<string>();
  const seen = new Set<string>();

  for (const record of records) {
    for (const fact of record.facts) {
      if (fact.reviewState === "omitido") continue;
      const { scope, key } = factLocation({
        category: record.category,
        recordId: record.id,
        fieldKey: fact.fieldKey,
      });
      seen.add(scope);
      if (isVerifiedState(fact.reviewState))
        fields.set(`${scope}|${key}`, factValue(fact));
      else unverified.add(scope);
    }
  }
  const rows = new Set([...seen].filter((scope) => !unverified.has(scope)));
  return { fields, rows };
}

/**
 * Etapas sem pendência: têm fatos a revisar e nenhuma pendência aberta.
 */
export function clearSteps<Step extends string>(
  records: readonly VerifiableRecord[],
  pendingTotals: Record<Step, number>,
  stepCategories: Record<Step, readonly string[]>,
): Record<Step, boolean> {
  const result = {} as Record<Step, boolean>;
  for (const step of Object.keys(stepCategories) as Step[]) {
    const hasFacts = records.some(
      (record) =>
        stepCategories[step].includes(record.category) &&
        record.facts.some((fact) => fact.reviewState !== "omitido"),
    );
    result[step] = hasFacts && (pendingTotals[step] ?? 0) === 0;
  }
  return result;
}
