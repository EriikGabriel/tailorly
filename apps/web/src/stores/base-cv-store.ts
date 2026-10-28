import { create } from "zustand";
import { useDocumentViewerStore } from "./document-viewer-store";
import { useQuickReviewStore } from "./quick-review-store";
import { useReviewDraftStore } from "./review-draft-store";

export type OwnerKind = "account" | "anonymous";

interface BaseCvState {
  file: File | null;
  selectionId: string | null;
  uploadedSelectionId: string | null;
  baseVersionId: string | null;
  /** De quem é o owner da versão enviada; define qual credencial a acessa. */
  ownerKind: OwnerKind | null;
  error: string | null;
  selectFile: (file: File | null) => boolean;
  setUploadedDocument: (
    file: File,
    baseVersionId?: string,
    ownerKind?: OwnerKind,
  ) => void;
  setOwnerKind: (ownerKind: OwnerKind | null) => void;
  setError: (error: string | null) => void;
  reset: () => void;
}

export const MAX_BASE_CV_BYTES = 7 * 1024 * 1024;

export const useBaseCvStore = create<BaseCvState>()((set, get) => ({
  file: null,
  selectionId: null,
  uploadedSelectionId: null,
  baseVersionId: null,
  ownerKind: null,
  error: null,
  selectFile: (file) => {
    if (file && (!/\.pdf$/i.test(file.name) || file.size === 0)) {
      set({ error: "Selecione um arquivo PDF que não esteja vazio." });
      return false;
    }

    if (file && file.size > MAX_BASE_CV_BYTES) {
      set({ error: "O PDF deve ter no máximo 7 MiB." });
      return false;
    }

    if (file === get().file) {
      set({ error: null });
      return true;
    }

    // A local selection ID is not a Ground Truth version or a content hash.
    const selectionId = file ? crypto.randomUUID() : null;
    useReviewDraftStore.getState().resetForSource(selectionId);
    useQuickReviewStore.getState().reset();
    useDocumentViewerStore.getState().reset();
    set({
      file,
      selectionId,
      uploadedSelectionId: null,
      baseVersionId: null,
      ownerKind: null,
      error: null,
    });
    return true;
  },
  setUploadedDocument: (file, baseVersionId, ownerKind) => {
    if (get().file !== file) return;
    set({
      uploadedSelectionId: get().selectionId,
      baseVersionId: baseVersionId ?? null,
      ownerKind: ownerKind ?? null,
    });
  },
  setOwnerKind: (ownerKind) => set({ ownerKind }),
  setError: (error) => set({ error }),
  reset: () => {
    useReviewDraftStore.getState().resetForSource(null);
    useQuickReviewStore.getState().reset();
    useDocumentViewerStore.getState().reset();
    set({
      file: null,
      selectionId: null,
      uploadedSelectionId: null,
      baseVersionId: null,
      ownerKind: null,
      error: null,
    });
  },
}));
