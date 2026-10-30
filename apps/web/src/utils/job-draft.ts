import type { JobInputMode } from "../types/job-draft";

/** Há uma vaga informada? Considera só o campo do modo ativo (link ou texto). */
export function hasJobTarget(
  mode: JobInputMode,
  jobUrl: string,
  jobText: string,
): boolean {
  return (mode === "url" ? jobUrl : jobText).trim().length > 0;
}
