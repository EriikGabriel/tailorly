import { create } from "zustand";

export type DocumentZoom = "fit" | number;

interface DocumentViewerState {
  zoom: DocumentZoom;
  /** Alterna entre o PDF original e os pontos de extração (bounding boxes) da IA. */
  boundingBoxesEnabled: boolean;
  setZoom: (zoom: DocumentZoom) => void;
  setBoundingBoxesEnabled: (enabled: boolean) => void;
  reset: () => void;
}

export const useDocumentViewerStore = create<DocumentViewerState>()((set) => ({
  zoom: "fit",
  boundingBoxesEnabled: false,
  setZoom: (zoom) => {
    if (zoom === "fit") set({ zoom });
    else if (Number.isFinite(zoom))
      set({ zoom: Math.min(200, Math.max(50, zoom)) });
  },
  setBoundingBoxesEnabled: (boundingBoxesEnabled) =>
    set({ boundingBoxesEnabled }),
  reset: () => set({ zoom: "fit", boundingBoxesEnabled: false }),
}));
