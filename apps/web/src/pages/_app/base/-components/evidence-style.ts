import type { ReviewStepId } from "@@types/review-model";

export const STEP_CATEGORIES: Record<ReviewStepId, readonly string[]> = {
  identity: ["identity"],
  experience: ["experience", "volunteering"],
  projects: ["projects"],
  education: ["degrees", "certifications"],
  skills: ["languages", "technologies"],
  preferences: [],
};

export const CATEGORY_LABELS: Record<string, string> = {
  identity: "Identidade",
  experience: "Experiência",
  volunteering: "Voluntariado",
  projects: "Projetos",
  degrees: "Formação",
  certifications: "Certificações",
  languages: "Idiomas",
  technologies: "Tecnologias",
};

export function categoryLabel(category: string): string {
  return CATEGORY_LABELS[category] ?? "Outros fatos";
}

export function isCategoryActive(category: string, activeStep: ReviewStepId) {
  return STEP_CATEGORIES[activeStep].includes(category);
}

/**
 * Paleta neutra para os pontos mapeados e vermelha (tom do design system) só
 * para o que está em foco agora — evita um arco-íris de cores por categoria
 * e deixa o "em foco" ser o único destaque forte na página.
 */
export const EVIDENCE_TONE = {
  box: {
    focused:
      "bg-secondary-container/40 shadow-md ring-2 ring-secondary-container/70",
    neutral: "bg-surface-container-low/40 ring-1 ring-outline-variant/40",
  },
  badge: {
    focused: "bg-tertiary-container text-on-error",
    neutral: "bg-surface-container-highest text-on-surface-variant",
  },
  // Sem mix-blend-mode: ele compõe com a seleção nativa do navegador (que
  // também é desenhada por cima do texto) e deixava o cursor/seleção
  // piscando ao arrastar. Translúcido simples já deixa a tinta escura do PDF
  // visível por baixo sem interferir na seleção de texto real.
  mark: {
    focused: "bg-secondary-fixed/60 border-secondary-fixed-dim border",
    neutral: "bg-on-surface-variant/20",
  },
} as const;
