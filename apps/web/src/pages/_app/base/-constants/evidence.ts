import type { ReviewStepId } from "@@types/review-model";

/** Categorias de fatos (backend) exibidas em cada etapa da revisão. */
export const STEP_CATEGORIES: Record<ReviewStepId, readonly string[]> = {
  identity: ["identity"],
  experience: ["experience", "volunteering"],
  projects: ["projects"],
  education: ["degrees", "certifications"],
  skills: ["languages", "technologies"],
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
  mark: {
    focused: "bg-secondary-fixed/60 border-secondary-fixed-dim border",
    neutral: "bg-on-surface-variant/20",
  },
} as const;

/**
 * Distância vertical (px) abaixo da qual duas marcações do mesmo registro são
 * tratadas como parte do mesmo bloco (ex.: linhas de um mesmo cargo), em vez
 * de dois blocos separados.
 */
export const CLUSTER_GAP = 36;
/** Folga (px) ao redor do bloco agrupado de um registro. */
export const GROUP_PADDING = 10;
/** Distância (px) máxima para unir marcações vizinhas num só retângulo. */
export const MERGE_GAP = 4;
