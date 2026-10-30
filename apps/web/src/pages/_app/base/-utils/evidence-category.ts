import type { ReviewStepId } from "@@types/review-model";
import { CATEGORY_LABELS, STEP_CATEGORIES } from "../-constants/evidence";
import type { EvidenceFact } from "../-types/evidence";

export function categoryLabel(category: string): string {
  return CATEGORY_LABELS[category] ?? "Outros fatos";
}

export function isCategoryActive(category: string, activeStep: ReviewStepId) {
  return STEP_CATEGORIES[activeStep].includes(category);
}

/**
 * Um fato está "em foco" quando o registro dele está aberto no formulário.
 * Categorias sem registro específico (ex.: identidade) acompanham a etapa
 * inteira; as repetíveis só entram em foco quando o cartão daquela entrada
 * está aberto no formulário à direita.
 */
export function isFactFocused(
  fact: EvidenceFact,
  activeStep: ReviewStepId,
  focusedRecordKeys: ReadonlySet<string>,
): boolean {
  if (!fact.recordKey) return isCategoryActive(fact.category, activeStep);
  return focusedRecordKeys.has(fact.recordKey);
}
