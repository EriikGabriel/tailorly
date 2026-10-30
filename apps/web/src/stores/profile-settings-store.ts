import type { AiPreferencesResponse } from "@lib/api/generated/model";
import { aiPreferencesUpdate } from "@lib/api/generated/tailorly";
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { InterpretationProvider } from "@/constants/ai-providers";
import type {
  AiPreferencesDraft,
  IdentityFields,
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

const defaultIdentity: IdentityFields = {
  fullName: "",
  email: "",
  phone: "",
  headline: "",
  location: "",
  linkedinUrl: "",
  githubUrl: "",
};

function isEqual<T>(a: T, b: T): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

interface ProfileSettingsStore {
  identityCommitted: IdentityFields;
  identityDraft: IdentityFields;

  aiCommitted: AiPreferencesDraft;
  aiDraft: AiPreferencesDraft;
  /** true após a primeira resposta real de GET /v1/ai-preferences. */
  aiLoaded: boolean;

  isDirty: boolean;
  isAiConfigValid: boolean;
  isSaving: boolean;
  saveError: string | null;

  updateIdentity: (patch: Partial<IdentityFields>) => void;
  updateProvider: (provider: InterpretationProvider) => void;
  updateRemote: (patch: Partial<RemoteDraft>) => void;
  updateOllama: (patch: Partial<OllamaDraft>) => void;

  hydrateAiPreferences: (response: AiPreferencesResponse) => void;

  save: () => Promise<void>;
  discard: () => void;
}

function recomputeDirty(state: ProfileSettingsStore): boolean {
  return (
    !isEqual(state.identityDraft, state.identityCommitted) ||
    !isEqual(state.aiDraft, state.aiCommitted)
  );
}

export const useProfileSettingsStore = create<ProfileSettingsStore>()(
  persist(
    (set, get) => ({
      identityCommitted: defaultIdentity,
      identityDraft: defaultIdentity,

      aiCommitted: defaultAiPreferencesDraft(),
      aiDraft: defaultAiPreferencesDraft(),
      aiLoaded: false,

      isDirty: false,
      isAiConfigValid: true,
      isSaving: false,
      saveError: null,

      updateIdentity: (patch) =>
        set((state) => {
          const identityDraft = { ...state.identityDraft, ...patch };
          const next = { ...state, identityDraft };
          return { identityDraft, isDirty: recomputeDirty(next) };
        }),
      updateProvider: (provider) =>
        set((state) => {
          const aiDraft = { ...state.aiDraft, provider };
          const next = { ...state, aiDraft };
          return {
            aiDraft,
            isDirty: recomputeDirty(next),
            isAiConfigValid: isAiDraftValid(aiDraft),
          };
        }),
      updateRemote: (patch) =>
        set((state) => {
          const aiDraft = {
            ...state.aiDraft,
            remote: { ...state.aiDraft.remote, ...patch },
          };
          const next = { ...state, aiDraft };
          return {
            aiDraft,
            isDirty: recomputeDirty(next),
            isAiConfigValid: isAiDraftValid(aiDraft),
          };
        }),
      updateOllama: (patch) =>
        set((state) => {
          const aiDraft = {
            ...state.aiDraft,
            ollama: { ...state.aiDraft.ollama, ...patch },
          };
          const next = { ...state, aiDraft };
          return {
            aiDraft,
            isDirty: recomputeDirty(next),
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
          let aiCommitted = state.aiCommitted;
          const aiDirty = !isEqual(state.aiDraft, state.aiCommitted);

          if (aiDirty && !isAiDraftValid(state.aiDraft)) {
            throw new Error(
              "Configuração de IA incompleta ou inválida. Revise endpoint, modelo e chave antes de salvar.",
            );
          }

          if (aiDirty) {
            const accessToken = useAuthStore.getState().accessToken;
            if (!accessToken) {
              throw new Error("Sessão expirada. Faça login novamente.");
            }

            const response = await aiPreferencesUpdate(
              aiPreferencesRequestFromDraft(state.aiDraft),
              { accessToken },
            );
            aiCommitted = aiDraftFromResponse(response);
          }

          set({
            identityCommitted: state.identityDraft,
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
          identityDraft: state.identityCommitted,
          aiDraft: state.aiCommitted,
          isDirty: false,
          isAiConfigValid: isAiDraftValid(state.aiCommitted),
          saveError: null,
        })),
    }),
    {
      name: "tailorly:profile-settings",
      // As preferências de IA vêm do backend (GET /v1/ai-preferences); só a
      // identidade (sem endpoint próprio ainda) é persistida localmente.
      partialize: (state) => ({ identityCommitted: state.identityCommitted }),
      merge: (persisted, current) => {
        const identityCommitted =
          (persisted as { identityCommitted?: IdentityFields })
            ?.identityCommitted ?? defaultIdentity;
        return {
          ...current,
          identityCommitted,
          identityDraft: identityCommitted,
        };
      },
    },
  ),
);
