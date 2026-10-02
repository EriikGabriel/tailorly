import { create } from "zustand";

export type DocumentZoom = "fit" | number;

interface DocumentViewerState {
  zoom: DocumentZoom;
  setZoom: (zoom: DocumentZoom) => void;
  reset: () => void;
}

export const useDocumentViewerStore = create<DocumentViewerState>()((set) => ({
  zoom: "fit",
  setZoom: (zoom) => {
    if (zoom === "fit") set({ zoom });
    else if (Number.isFinite(zoom))
      set({ zoom: Math.min(200, Math.max(50, zoom)) });
  },
  reset: () => set({ zoom: "fit" }),
}));
