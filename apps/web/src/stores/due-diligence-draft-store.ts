import { create } from "zustand";

/**
 * Rascunho do interruptor de due diligence automática, ainda não salvo. Fica fora do card para que a
 * barra de alterações do perfil salve ou descarte junto com as demais configurações. `null` = sem edição.
 */
interface DueDiligenceDraftStore {
  draft: boolean | null;
  setDraft: (draft: boolean | null) => void;
}

export const useDueDiligenceDraftStore = create<DueDiligenceDraftStore>()(
  (set) => ({
    draft: null,
    setDraft: (draft) => set({ draft }),
  }),
);
