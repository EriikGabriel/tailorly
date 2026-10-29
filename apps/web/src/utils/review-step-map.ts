import type { ReviewCategory } from "../types/review";
import type { ReviewStepId } from "../types/review-model";

const STEP_BY_CATEGORY: Record<ReviewCategory, ReviewStepId> = {
  identity: "identity",
  employment: "experience",
  volunteering: "experience",
  projects: "projects",
  education: "education",
  skills: "skills",
  languages: "skills",
  presentation: "preferences",
};

export function stepForReviewCategory(category: ReviewCategory): ReviewStepId {
  return STEP_BY_CATEGORY[category];
}

export interface StepPending {
  total: number;
  /** Quantas das pendências sustentam algo que a vaga pede. */
  relevant: number;
}

export const REVIEW_STEP_ORDER: readonly ReviewStepId[] = [
  "identity",
  "experience",
  "projects",
  "education",
  "skills",
  "preferences",
];

/** Contagem de pendências por etapa da auditoria, com as ligadas à vaga em separado. */
export function pendingByStep(
  issues: readonly { category: ReviewCategory; jobRelevance?: unknown }[],
): Record<ReviewStepId, StepPending> {
  const result = Object.fromEntries(
    REVIEW_STEP_ORDER.map((step) => [step, { total: 0, relevant: 0 }]),
  ) as Record<ReviewStepId, StepPending>;
  for (const issue of issues) {
    const entry = result[stepForReviewCategory(issue.category)];
    entry.total += 1;
    if (issue.jobRelevance) entry.relevant += 1;
  }
  return result;
}

/** Etapa a abrir primeiro: a que tem pendências ligadas à vaga, senão a primeira com pendências. */
export function firstStepWithPending(
  pending: Record<ReviewStepId, StepPending>,
): ReviewStepId | null {
  return (
    REVIEW_STEP_ORDER.find((step) => pending[step].relevant > 0) ??
    REVIEW_STEP_ORDER.find((step) => pending[step].total > 0) ??
    null
  );
}
