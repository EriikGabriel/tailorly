import { create } from "zustand";
import { readPointer } from "./session-pointer";

/**
 * Código Typst editado à mão, ligado à geração aberta. `content` é o rascunho; `savedContent` é o
 * último código gravado no servidor como template do owner (as próximas gerações o usam, a aberta
 * não muda). Fica em `sessionStorage` só para sobreviver a reload da aba: é rascunho de interface,
 * não fonte de verdade, e é apagado em `resetClientState`.
 */
interface TemplateDraft {
  generationId: string | null;
  content: string | null;
  savedContent: string | null;
}

interface TemplateDraftState extends TemplateDraft {
  setDraft: (generationId: string, content: string | null) => void;
  markSaved: (generationId: string, content: string) => void;
  reset: () => void;
}

export const TEMPLATE_DRAFT_KEY = "tailorly:template-draft";
const EMPTY: TemplateDraft = {
  generationId: null,
  content: null,
  savedContent: null,
};

const isNullableString = (value: unknown) =>
  value === null || typeof value === "string";

function isDraft(value: unknown): value is TemplateDraft {
  const item = value as Partial<TemplateDraft> | null;
  return (
    typeof item?.generationId === "string" &&
    isNullableString(item.content) &&
    isNullableString(item.savedContent)
  );
}

export const useTemplateDraftStore = create<TemplateDraftState>()((set) => ({
  ...(readPointer(TEMPLATE_DRAFT_KEY, isDraft) ?? EMPTY),
  setDraft: (generationId, content) =>
    set((state) => ({
      generationId,
      content,
      // Outra geração começa sem o "salvo" da anterior.
      savedContent:
        state.generationId === generationId ? state.savedContent : null,
    })),
  markSaved: (generationId, savedContent) =>
    set({ generationId, savedContent }),
  reset: () => set(EMPTY),
}));

useTemplateDraftStore.subscribe((state) => {
  try {
    if (state.generationId === null) {
      globalThis.sessionStorage?.removeItem(TEMPLATE_DRAFT_KEY);
    } else {
      const { generationId, content, savedContent } = state;
      globalThis.sessionStorage?.setItem(
        TEMPLATE_DRAFT_KEY,
        JSON.stringify({ generationId, content, savedContent }),
      );
    }
  } catch {
    // Sem armazenamento disponível o rascunho só deixa de sobreviver ao reload.
  }
});
