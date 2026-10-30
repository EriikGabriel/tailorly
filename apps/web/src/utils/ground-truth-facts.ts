// Agrupa os itens aprovados de um GtSnapshot (chaves planas "identity.name" ou
// "0:company" por categoria) na forma estruturada que a UI de perfil consome.
// Espelha as categorias e chaves aceitas pela revisão (CurriculumFactSchema, no backend).

import type { GtSnapshotItemResponse } from "@lib/api/generated/model";

export type IdentityFacts = Partial<
  Record<
    | "name"
    | "headline"
    | "location"
    | "email"
    | "phone"
    | "linkedin"
    | "github"
    | "website"
    | "summary",
    string
  >
>;

export type ExperienceFact = {
  company?: string;
  role?: string;
  employment?: string;
  status?: string;
  start?: string;
  end?: string;
  description?: string;
};

export type DegreeFact = {
  degree?: string;
  institution?: string;
  status?: string;
  start?: string;
  end?: string;
};

export type CertificationFact = {
  name?: string;
  issuer?: string;
  credential?: string;
  issued?: string;
  expires?: string;
};

export type LanguageFact = { language?: string; proficiency?: string };
export type TechnologyFact = { name?: string; context?: string };

export interface GroundTruthFacts {
  identity: IdentityFacts;
  experience: ExperienceFact[];
  degrees: DegreeFact[];
  certifications: CertificationFact[];
  languages: LanguageFact[];
  technologies: TechnologyFact[];
}

const ITEM_PREFIX = /^(\d{1,3}):(.+)$/;

function stringify(value: unknown): string | undefined {
  if (value === null || value === undefined) return undefined;
  const text = String(value).trim();
  return text.length > 0 ? text : undefined;
}

/** Agrupa os itens aprovados de um snapshot por categoria/índice, na ordem em que apareceram. */
export function groupSnapshotItems(
  items: GtSnapshotItemResponse[] | undefined,
): GroundTruthFacts {
  const identity: IdentityFacts = {};
  const lists: Record<string, Map<number, Record<string, string>>> = {};

  for (const item of items ?? []) {
    if (!item.category || !item.fieldKey) continue;
    const value = stringify(item.approvedValue);
    if (value === undefined) continue;

    if (item.category === "identity") {
      identity[item.fieldKey as keyof IdentityFacts] = value;
      continue;
    }

    const match = ITEM_PREFIX.exec(item.fieldKey);
    if (!match) continue;
    const index = Number(match[1]);
    const key = match[2];
    lists[item.category] ??= new Map();
    const bucket = lists[item.category];
    const record = bucket.get(index) ?? {};
    record[key] = value;
    bucket.set(index, record);
  }

  function toArray<T>(category: string): T[] {
    const bucket = lists[category];
    if (!bucket) return [];
    return Array.from(bucket.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([, record]) => record as T);
  }

  return {
    identity,
    experience: toArray<ExperienceFact>("experience"),
    degrees: toArray<DegreeFact>("degrees"),
    certifications: toArray<CertificationFact>("certifications"),
    languages: toArray<LanguageFact>("languages"),
    technologies: toArray<TechnologyFact>("technologies"),
  };
}
