import type { DraftCollection, ReviewStepId } from "@@types/review-model";
import { STEP_CATEGORIES } from "../-constants/evidence";

/**
 * Chaves dos registros abertos no formulário da etapa ativa. O card de uma entrada específica
 * (ex.: "Experiência 2") é o que define qual bounding box fica em foco no documento — não a
 * etapa inteira.
 */
export function focusedRecordKeysFor(
  activeStep: ReviewStepId,
  expandedRecords: Partial<Record<DraftCollection, string | null>>,
): Set<string> {
  return new Set(
    STEP_CATEGORIES[activeStep]
      .map((category) => expandedRecords[category as DraftCollection])
      .filter((id): id is string => Boolean(id)),
  );
}
