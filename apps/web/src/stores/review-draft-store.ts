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
  activeStep: ReviewStepId;
  expandedRecords: Partial<Record<DraftCollection, string | null>>;
  setActiveStep: (step: ReviewStepId) => void;
  setIdentity: (sourceId: string | null, fields: DraftFields) => void;
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
}

// Editable UI drafts only; these are never the server's approved snapshot.
export const useReviewDraftStore = create<ReviewDraftState>()((set) => ({
  sourceId: null,
  draft: createReviewDraft(),
  activeStep: "identity",
  expandedRecords: {},
  setActiveStep: (activeStep) => set({ activeStep }),
  setIdentity: (sourceId, identity) =>
    set((state) =>
      sourceId === state.sourceId
        ? { draft: { ...state.draft, identity } }
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
      activeStep: "identity",
      expandedRecords: {},
    }),
}));
