import type { IconProps } from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";

export const reviewSteps = [
  { id: "identity", title: "Identidade & Contato" },
  { id: "experience", title: "Experiência & Voluntariado" },
  { id: "projects", title: "Projetos & Métricas" },
  { id: "education", title: "Formação & Certificados" },
  { id: "skills", title: "Competências & Idiomas" },
  { id: "preferences", title: "Preferências" },
] as const;

export type ReviewStepId = (typeof reviewSteps)[number]["id"];
export type DraftFields = Record<string, string>;
export type DraftRecord = { id: string; fields: DraftFields };
/** Categorias repetíveis do rascunho: cada uma guarda uma lista de entradas. */
export const draftCollections = [
  "experience",
  "volunteering",
  "projects",
  "degrees",
  "certifications",
  "languages",
  "technologies",
] as const;
export type DraftCollection = (typeof draftCollections)[number];
export type ReviewDraft = {
  identity: DraftFields;
  experience: DraftRecord[];
  volunteering: DraftRecord[];
  projects: DraftRecord[];
  degrees: DraftRecord[];
  certifications: DraftRecord[];
  languages: DraftRecord[];
  technologies: DraftRecord[];
};

export const createReviewDraft = (): ReviewDraft => ({
  identity: {},
  experience: [],
  volunteering: [],
  projects: [],
  degrees: [],
  certifications: [],
  languages: [],
  technologies: [],
});

export type PeriodConfig = {
  startKey: string;
  endKey: string;
  /** Fato "situação" mantido em sincronia com o término (ex.: "Atual"/"Encerrado"). */
  statusKey?: string;
  startLabel: string;
  endLabel: string;
  /** Texto do interruptor "Atualmente..."; sem ele o término é sempre editável. */
  currentLabel?: string;
  currentStatus?: string;
  endedStatus?: string;
  /** Permite datas futuras no término (ex.: validade de certificação). */
  futureEnd?: boolean;
};

export type ReviewFieldType =
  | "text"
  | "email"
  | "tel"
  | "url"
  | "month"
  | "textarea"
  | "select"
  | "period"
  | "proficiency";

export type ReviewField = {
  key: string;
  label: string;
  type?: ReviewFieldType;
  hint?: string;
  wide?: boolean;
  icon?: ComponentType<IconProps>;
  placeholder?: string;
  maxLength?: number;
  options?: readonly { value: string; label: string }[];
  period?: PeriodConfig;
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
