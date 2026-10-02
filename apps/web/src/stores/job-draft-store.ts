import { create } from "zustand";
import { useQuickReviewStore } from "./quick-review-store";

export type JobInputMode = "url" | "text";
interface JobDraftState {
  mode: JobInputMode;
  jobUrl: string;
  jobText: string;
  revision: number;
  savedAt: number | null;
  setMode: (mode: JobInputMode) => void;
  setJobUrl: (url: string) => void;
  setJobText: (text: string) => void;
  saveDraft: () => void;
  reset: () => void;
}

export const useJobDraftStore = create<JobDraftState>()((set, get) => {
  function update(
    patch: Partial<Pick<JobDraftState, "mode" | "jobUrl" | "jobText">>,
  ) {
    if (
      Object.entries(patch).every(
        ([key, value]) => get()[key as keyof typeof patch] === value,
      )
    )
      return;
    useQuickReviewStore.getState().reset();
    set((state) => ({ ...patch, revision: state.revision + 1, savedAt: null }));
  }
  return {
    mode: "url",
    jobUrl: "",
    jobText: "",
    revision: 0,
    savedAt: null,
    setMode: (mode) => update({ mode }),
    setJobUrl: (jobUrl) => update({ jobUrl }),
    setJobText: (jobText) => update({ jobText }),
    saveDraft: () => set({ savedAt: Date.now() }),
    reset: () => {
      useQuickReviewStore.getState().reset();
      set((state) => ({
        mode: "url",
        jobUrl: "",
        jobText: "",
        revision: state.revision + 1,
        savedAt: null,
      }));
    },
  };
});
