import type { OwnerVaultInput } from "@queries/owner-vault";
import { create } from "zustand";

/**
 * Rascunho das preferências do Second Brain ainda não salvas. Fica fora do formulário para que a
 * barra de alterações da página de perfil (ProfileSaveBar) possa salvar ou descartar junto com as
 * configurações de IA. `null` significa que não há edição pendente.
 */
interface OwnerVaultDraftStore {
  draft: OwnerVaultInput | null;
  setDraft: (draft: OwnerVaultInput | null) => void;
}

export const useOwnerVaultDraftStore = create<OwnerVaultDraftStore>()(
  (set) => ({
    draft: null,
    setDraft: (draft) => set({ draft }),
  }),
);
