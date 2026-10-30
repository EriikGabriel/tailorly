import type { AiPreferencesResponse } from "@lib/api/generated/model";
import { aiPreferencesUpdate } from "@lib/api/generated/tailorly";
import { create } from "zustand";
import type { InterpretationProvider } from "@/constants/ai-providers";
import type {
  AiPreferencesDraft,
  OllamaDraft,
  RemoteDraft,
} from "@/types/profile-settings";
import {
  aiDraftFromResponse,
  aiPreferencesRequestFromDraft,
  defaultAiPreferencesDraft,
  isAiDraftValid,
} from "@/utils/ai-preferences";
import { useAuthStore } from "./auth-store";

function isEqual<T>(a: T, b: T): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

interface ProfileSettingsStore {
  aiCommitted: AiPreferencesDraft;
  aiDraft: AiPreferencesDraft;
  aiLoaded: boolean;

  isDirty: boolean;
  isAiConfigValid: boolean;
  isSaving: boolean;
  saveError: string | null;

  updateProvider: (provider: InterpretationProvider) => void;
  updateRemote: (patch: Partial<RemoteDraft>) => void;
  updateOllama: (patch: Partial<OllamaDraft>) => void;

  hydrateAiPreferences: (response: AiPreferencesResponse) => void;

  save: () => Promise<void>;
  discard: () => void;
}

export const useProfileSettingsStore = create<ProfileSettingsStore>()(
  (set, get) => ({
    aiCommitted: defaultAiPreferencesDraft(),
    aiDraft: defaultAiPreferencesDraft(),
    aiLoaded: false,

    isDirty: false,
    isAiConfigValid: true,
    isSaving: false,
    saveError: null,

    updateProvider: (provider) =>
      set((state) => {
        const aiDraft = { ...state.aiDraft, provider };
        return {
          aiDraft,
          isDirty: !isEqual(aiDraft, state.aiCommitted),
          isAiConfigValid: isAiDraftValid(aiDraft),
        };
      }),
    updateRemote: (patch) =>
      set((state) => {
        const aiDraft = {
          ...state.aiDraft,
          remote: { ...state.aiDraft.remote, ...patch },
        };
        return {
          aiDraft,
          isDirty: !isEqual(aiDraft, state.aiCommitted),
          isAiConfigValid: isAiDraftValid(aiDraft),
        };
      }),
    updateOllama: (patch) =>
      set((state) => {
        const aiDraft = {
          ...state.aiDraft,
          ollama: { ...state.aiDraft.ollama, ...patch },
        };
        return {
          aiDraft,
          isDirty: !isEqual(aiDraft, state.aiCommitted),
          isAiConfigValid: isAiDraftValid(aiDraft),
        };
      }),

    hydrateAiPreferences: (response) =>
      set((state) => {
        // Só hidrata na primeira carga: evita sobrescrever edição em
        // andamento se a query revalidar em segundo plano.
        if (state.aiLoaded) return state;
        const aiCommitted = aiDraftFromResponse(response);
        return {
          aiCommitted,
          aiDraft: aiCommitted,
          aiLoaded: true,
          isAiConfigValid: isAiDraftValid(aiCommitted),
        };
      }),

    save: async () => {
      const state = get();
      set({ isSaving: true, saveError: null });

      try {
        if (!isAiDraftValid(state.aiDraft)) {
          throw new Error(
            "Configuração de IA incompleta ou inválida. Revise endpoint, modelo e chave antes de salvar.",
          );
        }

        const accessToken = useAuthStore.getState().accessToken;
        if (!accessToken) {
          throw new Error("Sessão expirada. Faça login novamente.");
        }

        const response = await aiPreferencesUpdate(
          aiPreferencesRequestFromDraft(state.aiDraft),
          { accessToken },
        );
        const aiCommitted = aiDraftFromResponse(response);

        set({
          aiCommitted,
          aiDraft: aiCommitted,
          isDirty: false,
          isAiConfigValid: isAiDraftValid(aiCommitted),
          isSaving: false,
        });
      } catch (error) {
        set({
          isSaving: false,
          saveError:
            error instanceof Error
              ? error.message
              : "Não foi possível salvar as alterações.",
        });
        throw error;
      }
    },

    discard: () =>
      set((state) => ({
        aiDraft: state.aiCommitted,
        isDirty: false,
        isAiConfigValid: isAiDraftValid(state.aiCommitted),
        saveError: null,
      })),
  }),
);
