export type ReviewCategory =
  | "identity"
  | "employment"
  | "volunteering"
  | "projects"
  | "education"
  | "skills"
  | "languages";

export type CorrectionField = {
  label: string;
  type: "text" | "email" | "month" | "number" | "select";
  placeholder?: string;
  min?: string;
  max?: string;
  originalValue?: string;
  options?: readonly { value: string; label: string }[];
};

type ReviewChoiceBase = {
  value: string;
  label: string;
};

export type ReviewChoice =
  | (ReviewChoiceBase & {
      result: "confirmed" | "omitted";
      correction?: never;
    })
  | (ReviewChoiceBase & {
      result: "corrected";
      correction: CorrectionField;
    });

export type ReviewIssue = {
  id: string;
  category: ReviewCategory;
  type: "metric" | "temporal" | "fact";
  question: string;
  /** Nome legível do campo revisado (ex.: "Site pessoal"). */
  fieldLabel?: string;
  /** Valor atual do fato, exibido em destaque para conferência. */
  value?: string;
  evidence?: string;
  /** Fato de origem; permite refletir a resposta no rascunho da auditoria completa. */
  source?: {
    category: string;
    recordId: string;
    fieldKey: string;
    /** Valor atual do fato no servidor, no mesmo formato do rascunho da auditoria. */
    value: string;
  };
  /** Presente quando a vaga pede algo que este fato sustenta; define a prioridade na validação. */
  jobRelevance?: { importance: "required" | "preferred"; label: string };
  motivation: string;
  choices: readonly ReviewChoice[];
};

export type ReviewAnswer = { choice: string; correction: string };
export type ReviewAnswers = Record<string, ReviewAnswer>;
