import { create } from "zustand";
import type { OwnerKind } from "./base-cv-store";
import { persistPointer, readPointer } from "./session-pointer";

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

const POINTER_KEY = "tailorly:job-posting-selection";

function isSelected(value: unknown): value is SelectedJobPosting {
  const item = value as Partial<SelectedJobPosting> | null;
  return (
    typeof item?.postingId === "string" &&
    (item.ownerKind === "account" || item.ownerKind === "anonymous") &&
    (item.versionNumber === null || typeof item.versionNumber === "number")
  );
}

export const useJobPostingStore = create<JobPostingState>()((set) => ({
  selected: readPointer(POINTER_KEY, isSelected),
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

persistPointer(useJobPostingStore, POINTER_KEY, (state) => state.selected);
