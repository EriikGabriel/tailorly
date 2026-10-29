import { create } from "zustand";
import type { OwnerKind } from "./base-cv-store";

/** Só aponta para a vaga aberta; o conteúdo e as versões vivem no servidor. */
export interface SelectedJobPosting {
  postingId: string;
  ownerKind: OwnerKind;
  /** Nulo mostra a versão mais recente. */
  versionNumber: number | null;
}

interface JobPostingState {
  selected: SelectedJobPosting | null;
  select: (
    postingId: string,
    ownerKind: OwnerKind,
    versionNumber?: number | null,
  ) => void;
  setVersion: (versionNumber: number | null) => void;
  clear: () => void;
}

export const useJobPostingStore = create<JobPostingState>()((set) => ({
  selected: null,
  select: (postingId, ownerKind, versionNumber = null) =>
    set({ selected: { postingId, ownerKind, versionNumber } }),
  setVersion: (versionNumber) =>
    set((state) =>
      state.selected
        ? { selected: { ...state.selected, versionNumber } }
        : state,
    ),
  clear: () => set({ selected: null }),
}));
