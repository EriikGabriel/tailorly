import type { JobPostingVersionResponse } from "../lib/api/generated/model";
import type { OwnerKind } from "../stores/base-cv-store";

/** Vaga informada pela pessoa: o link da página ou o texto colado. */
export type JobPostingInput =
  | { mode: "url"; url: string }
  | { mode: "text"; text: string };

/** Versão gravada da vaga, com o tipo de dono (conta ou visitante) que a criou. */
export type AnalyzeResult = JobPostingVersionResponse & {
  ownerKind: OwnerKind;
};
