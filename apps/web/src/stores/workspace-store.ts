import { create } from "zustand";

export type WorkspaceView = "editor" | "preview";

interface WorkspaceState {
  activeView: WorkspaceView;
  setActiveView: (view: WorkspaceView) => void;
  reset: () => void;
}

export const useWorkspaceStore = create<WorkspaceState>()((set) => ({
  activeView: "editor",
  setActiveView: (activeView) => set({ activeView }),
  reset: () => set({ activeView: "editor" }),
}));
