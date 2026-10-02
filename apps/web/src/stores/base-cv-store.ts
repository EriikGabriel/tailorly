import { create } from "zustand";
import { useDocumentViewerStore } from "./document-viewer-store";
import { useQuickReviewStore } from "./quick-review-store";
import { useReviewDraftStore } from "./review-draft-store";

interface BaseCvState {
  file: File | null;
  selectionId: string | null;
  error: string | null;
  selectFile: (file: File | null, format?: "pdf" | "document") => boolean;
  setError: (error: string | null) => void;
  reset: () => void;
}

export const useBaseCvStore = create<BaseCvState>()((set, get) => ({
  file: null,
  selectionId: null,
  error: null,
  selectFile: (file, format = "document") => {
    const accepted = format === "pdf" ? /\.pdf$/i : /\.(pdf|doc|docx)$/i;
    if (file && (!accepted.test(file.name) || file.size === 0)) {
      set({
        error:
          format === "pdf"
            ? "Selecione um arquivo PDF que não esteja vazio."
            : "Selecione um arquivo PDF, DOC ou DOCX que não esteja vazio.",
      });
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
    set({ file, selectionId, error: null });
    return true;
  },
  setError: (error) => set({ error }),
  reset: () => {
    useReviewDraftStore.getState().resetForSource(null);
    useQuickReviewStore.getState().reset();
    useDocumentViewerStore.getState().reset();
    set({ file: null, selectionId: null, error: null });
  },
}));
