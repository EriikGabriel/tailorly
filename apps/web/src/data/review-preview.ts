import type { ReviewIssue } from "../types/review";

// Explicit demonstration data, never extracted from the selected document.
export const previewReviewIssues: readonly ReviewIssue[] = [
  {
    id: "employment-current",
    category: "skills",
    type: "temporal",
    question: "Você ainda atua como Tech Lead na FinTech Solutions?",
    evidence: "Tech Lead (2021–presente) — FinTech Solutions.",
    motivation: "A vaga pede disponibilidade integral ao longo do período.",
    choices: [
      { value: "current", label: "Sim, ainda atuo", result: "confirmed" },
      {
        value: "ended",
        label: "Não, o vínculo terminou",
        result: "corrected",
        correction: {
          label: "Mês e ano de saída",
          type: "month",
        },
      },
    ],
  },
  {
    id: "project-savings",
    category: "projects",
    type: "metric",
    question:
      "A economia com a migração de microsserviços foi de US$ 45.000 por ano?",
    evidence:
      "...conduziu programa FinOps com economia comprovada de $45.000/ano na nuvem AWS.",
    motivation:
      "A vaga valoriza fortemente eficiência em Cloud (FinOps). Confirmar essa métrica factual fortalece o alinhamento com os requisitos da vaga.",
    choices: [
      { value: "confirmed", label: "Confirmar valor", result: "confirmed" },
      {
        value: "edit",
        label: "Corrigir valor",
        result: "corrected",
        correction: {
          label: "Economia anual em US$",
          type: "number",
          min: "1",
          placeholder: "Novo valor anual",
          originalValue: "45000",
        },
      },
      { value: "omit", label: "Não incluir a métrica", result: "omitted" },
    ],
  },
];
