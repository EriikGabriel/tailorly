export const reviewSteps = [
  { id: "identity", title: "Identidade & Contato" },
  { id: "experience", title: "Experiência Profissional" },
  { id: "projects", title: "Projetos & Métricas" },
  { id: "education", title: "Formação & Certificados" },
  { id: "skills", title: "Competências & Idiomas" },
  { id: "preferences", title: "Preferências" },
] as const;

export type ReviewStepId = (typeof reviewSteps)[number]["id"];
export type DraftFields = Record<string, string>;
export type DraftRecord = { id: string; fields: DraftFields };
export type DraftCollection =
  | "experience"
  | "projects"
  | "degrees"
  | "certifications"
  | "languages"
  | "technologies";
export type ReviewDraft = {
  identity: DraftFields;
  experience: DraftRecord[];
  projects: DraftRecord[];
  degrees: DraftRecord[];
  certifications: DraftRecord[];
  languages: DraftRecord[];
  technologies: DraftRecord[];
};

export const createReviewDraft = (): ReviewDraft => ({
  identity: {},
  experience: [],
  projects: [],
  degrees: [],
  certifications: [],
  languages: [],
  technologies: [],
});

export type ReviewField = {
  key: string;
  label: string;
  type?: "text" | "email" | "tel" | "url" | "month" | "textarea";
  hint?: string;
  wide?: boolean;
};

export type CollectionProps = {
  collectionKey: DraftCollection;
  records: DraftRecord[];
  onChange: (records: DraftRecord[]) => void;
};

export type PresentationPreferences = {
  nameFormat: "short" | "full";
  language: "pt-BR" | "en-US" | "es-ES";
  links: { linkedin: boolean; github: boolean; whatsapp: boolean };
  hideOlderRoles: boolean;
};

export const initialPreferences: PresentationPreferences = {
  nameFormat: "short",
  language: "pt-BR",
  links: { linkedin: true, github: true, whatsapp: false },
  hideOlderRoles: true,
};
