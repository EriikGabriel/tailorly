import { create } from "zustand";
import type { ReviewAnswer, ReviewAnswers, ReviewIssue } from "../types/review";

interface QuickReviewState {
  open: boolean;
  contextKey: string | null;
  issues: readonly ReviewIssue[];
  preview: boolean;
  answers: ReviewAnswers;
  submittedPreview: { contextKey: string; answers: ReviewAnswers } | null;
  setOpen: (open: boolean) => void;
  configure: (
    contextKey: string,
    issues: readonly ReviewIssue[],
    preview: boolean,
  ) => void;
  setAnswer: (contextKey: string, id: string, answer: ReviewAnswer) => void;
  savePreview: (contextKey: string) => void;
  reset: () => void;
}

const defaults = () => ({
  open: false,
  contextKey: null,
  issues: [],
  preview: true,
  answers: {},
  submittedPreview: null,
});

export const useQuickReviewStore = create<QuickReviewState>()((set) => ({
  ...defaults(),
  setOpen: (open) => set({ open }),
  configure: (contextKey, issues, preview) =>
    set((state) =>
      state.contextKey === contextKey
        ? state
        : { contextKey, issues, preview, answers: {}, submittedPreview: null },
    ),
  setAnswer: (contextKey, id, answer) =>
    set((state) => {
      const issue = state.issues.find((item) => item.id === id);
      if (
        state.contextKey !== contextKey ||
        !issue?.choices.some((choice) => choice.value === answer.choice)
      )
        return state;
      return {
        answers: { ...state.answers, [id]: { ...answer } },
        submittedPreview: null,
      };
    }),
  savePreview: (contextKey) =>
    set((state) =>
      state.contextKey === contextKey
        ? {
            submittedPreview: {
              contextKey,
              answers: structuredClone(state.answers),
            },
          }
        : state,
    ),
  reset: () => set(defaults()),
}));
