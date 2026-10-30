import type { ApiReviewIssue } from "@@types/base-review";
import type { ReviewCategory, ReviewChoice, ReviewIssue } from "@/types/review";
import { factFieldLabel, formatFactValue } from "@/utils/fact-display";
import { type JobFactMatch, relevanceForFact } from "@/utils/job-fact-match";

// Categorias dos fatos (backend) -> seções da revisão.
const reviewCategoryByFactCategory: Record<string, ReviewCategory> = {
  identity: "identity",
  experience: "employment",
  volunteering: "volunteering",
  projects: "projects",
  degrees: "education",
  certifications: "education",
  languages: "languages",
  technologies: "skills",
};

const DEFAULT_CHOICES = ["confirmado", "corrigido", "omitido"];

/** Monta as opções de resposta da pendência (confirmar, corrigir ou omitir) a partir da configuração do servidor. */
function buildChoices(
  issue: ApiReviewIssue,
  fieldLabel: string,
  currentValue: string,
): ReviewChoice[] {
  const configured = issue.questionAndChoices ?? {};
  const rawChoices = Array.isArray(configured.choices)
    ? configured.choices
    : DEFAULT_CHOICES;
  return rawChoices.flatMap((raw): ReviewChoice[] => {
    if (raw === "confirmado")
      return [{ value: raw, label: "Está correto", result: "confirmed" }];
    if (raw === "corrigido")
      return [
        {
          value: raw,
          label: "Corrigir",
          result: "corrected",
          correction: {
            label: `Valor correto para ${fieldLabel}`,
            type: "text",
            placeholder: currentValue,
            originalValue: currentValue,
          },
        },
      ];
    if (raw === "omitido")
      return [
        { value: raw, label: "Não usar no currículo", result: "omitted" },
      ];
    return [];
  });
}

/** Converte a pendência crua da API na pendência exibida na interface, com a relevância para a vaga. */
export function toReviewIssue(
  issue: ApiReviewIssue,
  jobMatch: JobFactMatch | null,
): ReviewIssue {
  const fieldLabel = factFieldLabel(issue.fact?.fieldKey ?? "", issue.category);
  const currentValue = formatFactValue(
    issue.fact?.reviewedValue ?? issue.fact?.extractedValue,
  );
  return {
    id: issue.id,
    category: reviewCategoryByFactCategory[issue.category] ?? "identity",
    type: "fact",
    question: `${fieldLabel} está correto?`,
    fieldLabel,
    value: currentValue,
    evidence: issue.fact?.evidence[0]?.sourceExcerpt,
    source: issue.fact
      ? {
          category: issue.category,
          recordId: issue.fact.recordId,
          fieldKey: issue.fact.fieldKey,
          value: String(
            issue.fact.reviewedValue ?? issue.fact.extractedValue ?? "",
          ),
        }
      : undefined,
    jobRelevance:
      jobMatch && issue.fact
        ? relevanceForFact(jobMatch, issue.fact)
        : undefined,
    motivation: issue.reason,
    choices: buildChoices(issue, fieldLabel, currentValue),
  };
}

/** Só as pendências que a vaga menciona, com as obrigatórias primeiro. */
export function rankRelevantIssues(issues: ReviewIssue[]): ReviewIssue[] {
  return issues
    .filter((issue) => issue.jobRelevance)
    .sort(
      (a, b) =>
        Number(b.jobRelevance?.importance === "required") -
        Number(a.jobRelevance?.importance === "required"),
    );
}
