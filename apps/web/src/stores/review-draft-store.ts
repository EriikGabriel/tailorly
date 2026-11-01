import { create } from "zustand";
import {
  createReviewDraft,
  type DraftCollection,
  type DraftFields,
  type DraftRecord,
  type ReviewDraft,
  type ReviewStepId,
} from "../types/review-model";

interface ReviewDraftState {
  sourceId: string | null;
  draft: ReviewDraft;
  /** Último rascunho igual ao que está salvo na API; a diferença para `draft` é o que ainda não foi gravado. */
  baseline: ReviewDraft;
  activeStep: ReviewStepId;
  expandedRecords: Partial<Record<DraftCollection, string | null>>;
  setActiveStep: (step: ReviewStepId) => void;
  setIdentity: (sourceId: string | null, fields: DraftFields) => void;
  hydrateFromApi: (sourceId: string | null, draft: ReviewDraft) => void;
  setCollection: (
    sourceId: string | null,
    collection: DraftCollection,
    records: DraftRecord[],
  ) => void;
  setExpandedRecord: (
    sourceId: string | null,
    collection: DraftCollection,
    id: string | null,
  ) => void;
  resetForSource: (sourceId: string | null) => void;
  /** Marca o rascunho enviado à API como salvo, sem apagar o que foi digitado enquanto salvava. */
  markSaved: (saved: ReviewDraft) => void;
  discardChanges: () => void;
}

// Editable UI drafts only; these are never the server's approved snapshot.
export const useReviewDraftStore = create<ReviewDraftState>()((set) => ({
  sourceId: null,
  draft: createReviewDraft(),
  baseline: createReviewDraft(),
  activeStep: "identity",
  expandedRecords: {},
  setActiveStep: (activeStep) => set({ activeStep }),
  setIdentity: (sourceId, identity) =>
    set((state) =>
      sourceId === state.sourceId
        ? { draft: { ...state.draft, identity } }
        : state,
    ),
  hydrateFromApi: (sourceId, draft) =>
    set((state) =>
      sourceId === state.sourceId &&
      Object.values(state.draft.identity).every((value) => !value) &&
      Object.values(state.draft)
        .filter(Array.isArray)
        .every((records) => records.length === 0)
        ? { draft, baseline: draft }
        : state,
    ),
  setCollection: (sourceId, collection, records) =>
    set((state) =>
      sourceId === state.sourceId
        ? { draft: { ...state.draft, [collection]: records } }
        : state,
    ),
  setExpandedRecord: (sourceId, collection, id) =>
    set((state) =>
      sourceId === state.sourceId
        ? { expandedRecords: { ...state.expandedRecords, [collection]: id } }
        : state,
    ),
  resetForSource: (sourceId) =>
    set({
      sourceId,
      draft: createReviewDraft(),
      baseline: createReviewDraft(),
      activeStep: "identity",
      expandedRecords: {},
    }),
  markSaved: (baseline) => set({ baseline }),
  discardChanges: () => set((state) => ({ draft: state.baseline })),
}));

/** Há edições no rascunho que ainda não foram gravadas na API? */
export function hasUnsavedReviewChanges(
  state: Pick<ReviewDraftState, "draft" | "baseline">,
): boolean {
  return JSON.stringify(state.draft) !== JSON.stringify(state.baseline);
}
