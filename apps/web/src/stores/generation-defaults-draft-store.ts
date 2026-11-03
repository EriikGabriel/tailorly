import {
  type CvLanguagePreference,
  DEFAULT_LANGUAGE,
  toLanguagePreference,
} from "@constants/generation-options";
import {
  DEFAULT_TEMPLATE,
  type GenerationDefaults,
} from "@queries/generation-defaults";
import { availableGuidelines } from "@stores/generation-preferences-store";
import { create } from "zustand";

/** Padrões de geração editados no perfil; o layout nunca é nulo aqui (sem escolha vale o clássico). */
export interface GenerationDefaultsDraft {
  templateName: string;
  optimizeForAts: boolean;
  guidelines: string[];
  language: CvLanguagePreference;
}

export function draftFromSaved(
  saved: GenerationDefaults,
): GenerationDefaultsDraft {
  return {
    templateName: saved.templateName ?? DEFAULT_TEMPLATE,
    optimizeForAts: saved.optimizeForAts,
    guidelines: saved.guidelines,
    language: toLanguagePreference(saved.language),
  };
}

export const defaultGenerationDraft = (): GenerationDefaultsDraft => ({
  templateName: DEFAULT_TEMPLATE,
  optimizeForAts: true,
  guidelines: availableGuidelines.slice(0, 3),
  language: DEFAULT_LANGUAGE,
});

/**
 * Rascunho dos padrões de geração ainda não salvos. Fica fora do card para que a barra de
 * alterações do perfil (ProfileSaveBar) salve ou descarte junto com as demais configurações.
 * `null` significa que não há edição pendente.
 */
interface GenerationDefaultsDraftStore {
  draft: GenerationDefaultsDraft | null;
  setDraft: (draft: GenerationDefaultsDraft | null) => void;
}

export const useGenerationDefaultsDraftStore =
  create<GenerationDefaultsDraftStore>()((set) => ({
    draft: null,
    setDraft: (draft) => set({ draft }),
  }));

const sameSet = (left: string[], right: string[]) =>
  left.length === right.length && left.every((item) => right.includes(item));

/** Voltar ao valor salvo não conta como alteração; a ordem das diretrizes não importa. */
export function isGenerationDraftDirty(
  draft: GenerationDefaultsDraft | null,
  saved: GenerationDefaultsDraft | undefined,
): boolean {
  if (!draft || !saved) return false;
  return (
    draft.templateName !== saved.templateName ||
    draft.optimizeForAts !== saved.optimizeForAts ||
    draft.language !== saved.language ||
    !sameSet(draft.guidelines, saved.guidelines)
  );
}
