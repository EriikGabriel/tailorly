import { create } from "zustand";
import type { OwnerKind } from "./base-cv-store";

/** Só aponta para a geração aberta; o conteúdo e os arquivos vivem no servidor. */
export interface SelectedGeneration {
  id: string;
  ownerKind: OwnerKind;
}

interface CvGenerationState {
  selected: SelectedGeneration | null;
  select: (id: string, ownerKind: OwnerKind) => void;
  clear: () => void;
}

export const useCvGenerationStore = create<CvGenerationState>()((set) => ({
  selected: null,
  select: (id, ownerKind) => set({ selected: { id, ownerKind } }),
  clear: () => set({ selected: null }),
}));
