import type {
  JobAnalysis,
  JobPostingResponse,
  Requirement,
} from "@lib/api/generated/model";

export type GroupedRequirements = {
  required: Requirement[];
  preferred: Requirement[];
};

/** Quando a IA não rodou, usa a divisão por títulos do texto como requisitos sem categoria. */
function fallbackRequirements(
  details: JobPostingResponse,
): GroupedRequirements {
  const toRequirements = (
    items: readonly string[] | undefined,
    importance: string,
  ): Requirement[] =>
    (items ?? []).map((text) => ({ text, importance, evidence: text }));
  return {
    required: toRequirements(details.requirements, "required"),
    preferred: toRequirements(details.niceToHave, "preferred"),
  };
}

/** Separa os requisitos em obrigatórios e desejáveis, com a análise por IA quando existe. */
export function groupRequirements(
  details: JobPostingResponse,
  analysis: JobAnalysis | undefined,
): GroupedRequirements {
  if (!analysis) return fallbackRequirements(details);
  const requirements = analysis.requirements ?? [];
  return {
    required: requirements.filter((item) => item.importance === "required"),
    preferred: requirements.filter((item) => item.importance === "preferred"),
  };
}
