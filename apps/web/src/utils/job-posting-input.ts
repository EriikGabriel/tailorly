import type { JobPostingInput } from "../types/job-posting";
import { checkJobUrl } from "./job-url";

/** Limite de texto colado aceito pelo servidor (`CreateJobPostingRequest.text`). */
export const MAX_JOB_TEXT_LENGTH = 50_000;

/**
 * Motivo pelo qual a vaga não deve ser enviada, ou `null` se pode. Roda antes de qualquer chamada
 * (inclusive a que emitiria uma sessão anônima), para não gastar requisição, cota nem a sessão
 * com uma entrada que o servidor recusaria.
 */
export function jobPostingInputError(input: JobPostingInput): string | null {
  if (input.mode === "url") {
    const check = checkJobUrl(input.url);
    if (check.status === "empty") return "Informe o link da vaga.";
    return check.status === "invalid" ? check.message : null;
  }
  if (input.text.trim().length === 0) return "Cole o texto da vaga.";
  if (input.text.length > MAX_JOB_TEXT_LENGTH)
    return `O texto da vaga passa de ${MAX_JOB_TEXT_LENGTH} caracteres.`;
  return null;
}
