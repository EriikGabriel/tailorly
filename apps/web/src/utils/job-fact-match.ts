import type { JobAnalysis, Requirement } from "../lib/api/generated/model";

/** Só o que o cruzamento lê; `BaseFact` e `FactRecord` da revisão satisfazem estes formatos. */
export interface MatchableFact {
  id: string;
  recordId: string;
  /** Itens de lista usam o prefixo do índice, como "0:company"; identidade não tem prefixo. */
  fieldKey: string;
  extractedValue: unknown;
  reviewedValue: unknown;
  reviewState: string;
}

export interface MatchableRecord {
  id: string;
  facts: MatchableFact[];
}

export type RequirementStatus = "supported" | "unconfirmed" | "missing";
export type Importance = "required" | "preferred";

export interface RequirementMatch {
  requirement: Requirement;
  importance: Importance;
  /** Tecnologias/competências nomeadas pela vaga, usadas na busca nos fatos. */
  terms: string[];
  status: RequirementStatus;
  factIds: string[];
}

export interface FactRelevance {
  importance: Importance;
  /** Frase para a pessoa entender por que o fato foi pedido na validação. */
  label: string;
}

export interface JobFactMatch {
  matches: RequirementMatch[];
  /** Relevância por fato: o próprio fato cita o requisito. */
  byFact: ReadonlyMap<string, FactRelevance>;
  /**
   * Relevância por item de lista: os demais fatos do mesmo item (empresa, cargo e datas de uma
   * experiência que cita o requisito) dão contexto. Um registro agrupa uma categoria inteira, então
   * itens vizinhos não herdam a relevância.
   */
  byItem: ReadonlyMap<string, FactRelevance>;
}

/**
 * Texto comparável: minúsculo, sem acentos, pontuação vira espaço, exceto `+`, `#` e pontos
 * internos de nomes como "node.js" e "c++".
 */
export function normalizeForMatch(value: string): string {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .replace(/[^a-z0-9+#.]+/g, " ")
    .replace(/\.(?=\s|$)/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function variants(term: string): string[] {
  const normalized = normalizeForMatch(term);
  return [
    ...new Set([
      normalized,
      normalized.replace(/\./g, ""),
      normalized.replace(/[.\s]/g, ""),
    ]),
  ].filter(Boolean);
}

function containsTerm(haystack: string, term: string): boolean {
  const padded = ` ${haystack} `;
  return variants(term).some((variant) => padded.includes(` ${variant} `));
}

function valueText(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value;
  if (typeof value === "number" || typeof value === "boolean")
    return String(value);
  if (Array.isArray(value)) return value.map(valueText).join(" ");
  if (typeof value === "object")
    return Object.values(value as Record<string, unknown>)
      .map(valueText)
      .join(" ");
  return "";
}

function factText(fact: MatchableFact): string {
  return normalizeForMatch(
    valueText(fact.reviewedValue ?? fact.extractedValue),
  );
}

/** Chave do item de lista a que o fato pertence; nula para fatos sem índice (identidade). */
function itemKey(fact: Pick<MatchableFact, "recordId" | "fieldKey">) {
  const index = /^(\d+):/.exec(fact.fieldKey)?.[1];
  return index === undefined ? null : `${fact.recordId}#${index}`;
}

function rank(importance: Importance) {
  return importance === "required" ? 0 : 1;
}

function better(
  current: FactRelevance | undefined,
  next: FactRelevance,
): FactRelevance {
  return !current || rank(next.importance) < rank(current.importance)
    ? next
    : current;
}

function uniqueTerms(terms: readonly string[] | undefined): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const term of terms ?? []) {
    const key = normalizeForMatch(term);
    if (key && !seen.has(key)) {
      seen.add(key);
      result.push(term.trim());
    }
  }
  return result;
}

/**
 * Cruza os requisitos da vaga com os fatos do Currículo Base para decidir o que vale validar
 * antes de gerar. Só o que a vaga nomeia (tecnologias e competências) é buscado nos fatos;
 * requisitos sem nome identificável não geram relevância, porque adivinhar a relação entre uma
 * frase e um fato seria inferir. Fatos omitidos nunca contam como evidência.
 */
export function matchJobToFacts(
  analysis: JobAnalysis | undefined,
  records: readonly MatchableRecord[],
): JobFactMatch {
  const facts = records.flatMap((record) =>
    record.facts
      .filter((fact) => fact.reviewState !== "omitido")
      .map((fact) => ({ fact, text: factText(fact) })),
  );

  const covered = new Set<string>();
  const items: { requirement: Requirement; importance: Importance }[] = [];
  for (const requirement of analysis?.requirements ?? []) {
    const importance: Importance =
      requirement.importance === "preferred" ? "preferred" : "required";
    items.push({ requirement, importance });
    for (const term of requirement.technologies ?? [])
      covered.add(normalizeForMatch(term));
  }
  // Tecnologias citadas fora de um requisito (por exemplo, nas responsabilidades) entram como desejáveis.
  for (const technology of uniqueTerms(analysis?.technologies)) {
    if (covered.has(normalizeForMatch(technology))) continue;
    items.push({
      requirement: {
        text: technology,
        importance: "preferred",
        category: "technology",
        technologies: [technology],
        evidence: technology,
      },
      importance: "preferred",
    });
  }

  const byFact = new Map<string, FactRelevance>();
  const byItem = new Map<string, FactRelevance>();

  const matches: RequirementMatch[] = items.map(
    ({ requirement, importance }) => {
      const terms = uniqueTerms(requirement.technologies);
      const hits = facts.filter(({ text }) =>
        terms.some((term) => containsTerm(text, term)),
      );
      const factIds = hits.map(({ fact }) => fact.id);
      const supported = hits.some(
        ({ fact }) =>
          fact.reviewState === "confirmado" || fact.reviewState === "corrigido",
      );

      for (const { fact, text } of hits) {
        const named = terms.filter((term) => containsTerm(text, term));
        byFact.set(
          fact.id,
          better(byFact.get(fact.id), {
            importance,
            label: `A vaga pede ${named.join(", ")}`,
          }),
        );
        const key = itemKey(fact);
        if (key)
          byItem.set(
            key,
            better(byItem.get(key), {
              importance,
              label: `Faz parte do item que cita ${named.join(", ")}, pedido pela vaga`,
            }),
          );
      }

      return {
        requirement,
        importance,
        terms,
        status:
          terms.length === 0 || hits.length === 0
            ? "missing"
            : supported
              ? "supported"
              : "unconfirmed",
        factIds,
      };
    },
  );

  return { matches, byFact, byItem };
}

/** Requisitos que dá para verificar nos fatos: os que a vaga nomeia por tecnologia ou competência. */
export function verifiableMatches(match: JobFactMatch): RequirementMatch[] {
  return match.matches
    .filter((item) => item.terms.length > 0)
    .sort((a, b) => rank(a.importance) - rank(b.importance));
}

export function relevanceForFact(
  match: JobFactMatch,
  fact: Pick<MatchableFact, "id" | "recordId" | "fieldKey">,
): FactRelevance | undefined {
  const key = itemKey(fact);
  return match.byFact.get(fact.id) ?? (key ? match.byItem.get(key) : undefined);
}

/**
 * Quantas pendências a revisão rápida mostraria: as ligadas à vaga quando ela nomeia algo
 * verificável nos fatos, senão todas as pendências. Mesma conta de `quickIssues.length` no
 * diálogo, mas sobre os dados crus da API — serve para decidir a navegação antes de montar
 * (ou não) a tela cheia da validação.
 */
export function countQuickReviewIssues(
  analysis: JobAnalysis | undefined,
  records: readonly MatchableRecord[],
  pendingFacts: readonly Pick<MatchableFact, "id" | "recordId" | "fieldKey">[],
): number {
  const match = matchJobToFacts(analysis, records);
  const canPrioritize = verifiableMatches(match).length > 0;
  return canPrioritize
    ? pendingFacts.filter((fact) => relevanceForFact(match, fact)).length
    : pendingFacts.length;
}
