import type { CvGenerationResponse } from "@lib/api/generated/model";
import { ATS_ACCEPTABLE_SCORE, ATS_TARGET_SCORE } from "@/utils/ats-score";

export type AtsBand = "all" | "great" | "acceptable" | "low" | "none";
export type PeriodFilter = "all" | "7" | "30" | "90";
export type SortKey = "recent" | "oldest" | "ats-desc" | "ats-asc" | "company";

export interface CvFilters {
  query: string;
  company: string;
  ats: AtsBand;
  period: PeriodFilter;
  sort: SortKey;
}

export const defaultFilters: CvFilters = {
  query: "",
  company: "",
  ats: "all",
  period: "all",
  sort: "recent",
};

export const atsBandOptions: { value: AtsBand; label: string }[] = [
  { value: "all", label: "Qualquer nota ATS" },
  { value: "great", label: `Ótima (${ATS_TARGET_SCORE}+)` },
  {
    value: "acceptable",
    label: `Aceitável (${ATS_ACCEPTABLE_SCORE}–${ATS_TARGET_SCORE - 1})`,
  },
  { value: "low", label: `Abaixo de ${ATS_ACCEPTABLE_SCORE}` },
  { value: "none", label: "Sem nota" },
];

export const periodOptions: { value: PeriodFilter; label: string }[] = [
  { value: "all", label: "Qualquer data" },
  { value: "7", label: "Últimos 7 dias" },
  { value: "30", label: "Últimos 30 dias" },
  { value: "90", label: "Últimos 90 dias" },
];

export const sortOptions: { value: SortKey; label: string }[] = [
  { value: "recent", label: "Mais recentes" },
  { value: "oldest", label: "Mais antigos" },
  { value: "ats-desc", label: "Maior nota ATS" },
  { value: "ats-asc", label: "Menor nota ATS" },
  { value: "company", label: "Empresa (A–Z)" },
];

const DAY_MS = 24 * 60 * 60 * 1000;

export function atsScoreOf(cv: CvGenerationResponse): number | undefined {
  return cv.ats?.report?.score;
}

function timeOf(cv: CvGenerationResponse): number {
  return cv.createdAt ? new Date(cv.createdAt).getTime() : 0;
}

function normalize(text: string): string {
  return text
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase();
}

function matchesBand(score: number | undefined, band: AtsBand): boolean {
  if (band === "all") return true;
  if (score === undefined) return band === "none";
  if (band === "great") return score >= ATS_TARGET_SCORE;
  if (band === "acceptable")
    return score >= ATS_ACCEPTABLE_SCORE && score < ATS_TARGET_SCORE;
  if (band === "low") return score < ATS_ACCEPTABLE_SCORE;
  return false;
}

/** Empresas distintas do histórico, em ordem alfabética, para o filtro por empresa. */
export function companiesOf(items: CvGenerationResponse[]): string[] {
  const names = new Set<string>();
  for (const cv of items) {
    const name = cv.job?.companyName?.trim();
    if (name) names.add(name);
  }
  return [...names].sort((a, b) => a.localeCompare(b, "pt-BR"));
}

export function hasActiveFilters(filters: CvFilters): boolean {
  return (
    filters.query.trim() !== "" ||
    filters.company !== "" ||
    filters.ats !== "all" ||
    filters.period !== "all"
  );
}

/** Busca por vaga, empresa, template e termos cobertos; aplica filtros e ordena. */
export function applyFilters(
  items: CvGenerationResponse[],
  filters: CvFilters,
  now = Date.now(),
): CvGenerationResponse[] {
  const terms = normalize(filters.query).split(/\s+/).filter(Boolean);
  const since =
    filters.period === "all" ? 0 : now - Number(filters.period) * DAY_MS;

  const filtered = items.filter((cv) => {
    if (filters.company && cv.job?.companyName?.trim() !== filters.company)
      return false;
    if (!matchesBand(atsScoreOf(cv), filters.ats)) return false;
    if (since && timeOf(cv) < since) return false;
    if (terms.length === 0) return true;
    const haystack = normalize(
      [
        cv.job?.title,
        cv.job?.companyName,
        cv.template?.name,
        ...(cv.ats?.report?.matchedTerms ?? []),
      ]
        .filter(Boolean)
        .join(" "),
    );
    return terms.every((term) => haystack.includes(term));
  });

  return filtered.sort((a, b) => {
    switch (filters.sort) {
      case "oldest":
        return timeOf(a) - timeOf(b);
      case "ats-desc":
      case "ats-asc": {
        const direction = filters.sort === "ats-desc" ? -1 : 1;
        const scoreA = atsScoreOf(a);
        const scoreB = atsScoreOf(b);
        if (scoreA === undefined && scoreB === undefined) return 0;
        if (scoreA === undefined) return 1;
        if (scoreB === undefined) return -1;
        return (scoreA - scoreB) * direction;
      }
      case "company":
        return (a.job?.companyName ?? "￿").localeCompare(
          b.job?.companyName ?? "￿",
          "pt-BR",
        );
      default:
        return timeOf(b) - timeOf(a);
    }
  });
}

export interface CvStats {
  total: number;
  companies: number;
  averageScore: number | null;
  bestScore: number | null;
  meetingTarget: number;
}

export function statsOf(items: CvGenerationResponse[]): CvStats {
  const scores = items
    .map(atsScoreOf)
    .filter((score): score is number => score !== undefined);
  return {
    total: items.length,
    companies: companiesOf(items).length,
    averageScore: scores.length
      ? Math.round(
          scores.reduce((sum, score) => sum + score, 0) / scores.length,
        )
      : null,
    bestScore: scores.length ? Math.max(...scores) : null,
    meetingTarget: scores.filter((score) => score >= ATS_TARGET_SCORE).length,
  };
}
