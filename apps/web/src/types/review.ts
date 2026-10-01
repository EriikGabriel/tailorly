export type ReviewCategory =
  | "identity"
  | "employment"
  | "projects"
  | "education"
  | "skills"
  | "languages"
  | "presentation";

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
  type: "metric" | "temporal";
  question: string;
  evidence?: string;
  motivation: string;
  choices: readonly ReviewChoice[];
};

export type ReviewAnswer = { choice: string; correction: string };
export type ReviewAnswers = Record<string, ReviewAnswer>;
