import { create } from "zustand";
import type { OwnerKind } from "./base-cv-store";

/** Só aponta para a geração aberta; o conteúdo e os arquivos vivem no servidor. */
export interface SelectedGeneration {
  id: string;
  ownerKind: OwnerKind;
}

interface CvGenerationState {
  selected: SelectedGeneration | null;
  /** Geração recém-criada cuja leitura da IA ainda deve rodar sozinha (uma única vez). */
  autoReviewId: string | null;
  requestAutoReview: (id: string) => void;
  consumeAutoReview: (id: string) => boolean;
  select: (id: string, ownerKind: OwnerKind) => void;
  clear: () => void;
}

export const useCvGenerationStore = create<CvGenerationState>()((set, get) => ({
  selected: null,
  autoReviewId: null,
  requestAutoReview: (id) => set({ autoReviewId: id }),
  consumeAutoReview: (id) => {
    if (get().autoReviewId !== id) return false;
    set({ autoReviewId: null });
    return true;
  },
  select: (id, ownerKind) => set({ selected: { id, ownerKind } }),
  clear: () => set({ selected: null, autoReviewId: null }),
}));
