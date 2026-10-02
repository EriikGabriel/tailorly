import { create } from "zustand";
import {
  initialPreferences,
  type PresentationPreferences,
} from "../types/review-model";

const defaults = (): PresentationPreferences => ({
  ...initialPreferences,
  links: { ...initialPreferences.links },
});

interface PresentationState {
  preferences: PresentationPreferences;
  setPreferences: (preferences: PresentationPreferences) => void;
  reset: () => void;
}

export const usePresentationStore = create<PresentationState>()((set) => ({
  preferences: defaults(),
  setPreferences: (preferences) =>
    set({ preferences: { ...preferences, links: { ...preferences.links } } }),
  reset: () => set({ preferences: defaults() }),
}));
