import type { ReviewAnswer, ReviewCategory, ReviewIssue } from "@/types/review";

export const reviewCategories = {
  identity: "Identificação e contato",
  employment: "Experiência profissional",
  projects: "Projetos e resultados",
  education: "Formação e certificações",
  skills: "Competências",
  languages: "Idiomas",
  presentation: "Apresentação do currículo",
} as const satisfies Record<ReviewCategory, string>;

export function selectQuickReviewIssues(issues: readonly ReviewIssue[]) {
  const selected: ReviewIssue[] = [];
  const categories = new Set<ReviewCategory>();

  for (const issue of issues) {
    if (selected.length === 3) break;
    if (!categories.has(issue.category) && categories.size === 2) continue;
    categories.add(issue.category);
    selected.push(issue);
  }

  return selected;
}

export function isReviewIssueComplete(
  issue: ReviewIssue,
  answer?: ReviewAnswer,
) {
  if (!answer) return false;
  const choice = issue.choices.find((item) => item.value === answer.choice);
  if (!choice) return false;
  if (!choice.correction) return true;

  const value = answer.correction.trim();
  if (!value || value === choice.correction.originalValue) return false;

  switch (choice.correction.type) {
    case "month": {
      const max = choice.correction.max ?? new Date().toISOString().slice(0, 7);
      return (
        /^\d{4}-(0[1-9]|1[0-2])$/.test(value) &&
        value >= (choice.correction.min ?? "0000-01") &&
        value <= max
      );
    }
    case "number":
      return (
        Number.isFinite(Number(value)) &&
        Number(value) >= Number(choice.correction.min ?? 0) &&
        (choice.correction.max === undefined ||
          Number(value) <= Number(choice.correction.max)) &&
        (choice.correction.originalValue === undefined ||
          Number(value) !== Number(choice.correction.originalValue))
      );
    case "email":
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    case "select":
      return (
        choice.correction.options?.some((option) => option.value === value) ??
        false
      );
    case "text":
      return true;
  }
}
