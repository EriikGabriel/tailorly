import type { AiPreferencesResponse } from "@lib/api/generated/model";
import { aiPreferencesUpdate } from "@lib/api/generated/tailorly";
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { InterpretationProvider } from "@/data/profile-preview";
import { previewInterpretationSettings } from "@/data/profile-preview";
import { useAuthStore } from "./auth-store";

export interface IdentityFields {
  fullName: string;
  email: string;
  phone: string;
  headline: string;
  location: string;
  linkedinUrl: string;
  githubUrl: string;
}

export interface RemoteDraft {
  endpoint: string;
  model: string;
  /** Sempre "" ao carregar: o backend nunca devolve a chave em texto puro. */
  apiKey: string;
}

export interface OllamaDraft {
  endpoint: string;
  model: string;
}

export interface AiPreferencesDraft {
  provider: InterpretationProvider;
  remote: RemoteDraft;
  ollama: OllamaDraft;
  remoteApiKeyConfigured: boolean;
  ollamaApiKeyConfigured: boolean;
}

const defaultIdentity: IdentityFields = {
  fullName: "",
  email: "",
  phone: "",
  headline: "",
  location: "",
  linkedinUrl: "",
  githubUrl: "",
};

function defaultAiPreferencesDraft(): AiPreferencesDraft {
  return {
    provider: previewInterpretationSettings.provider,
    remote: {
      endpoint: previewInterpretationSettings.remote.endpoint,
      model: previewInterpretationSettings.remote.model,
      apiKey: "",
    },
    ollama: {
      endpoint: previewInterpretationSettings.ollama.endpoint,
      model: previewInterpretationSettings.ollama.model,
    },
    remoteApiKeyConfigured: false,
    ollamaApiKeyConfigured: false,
  };
}

function aiDraftFromResponse(
  response: AiPreferencesResponse,
): AiPreferencesDraft {
  return {
    provider: response.provider as InterpretationProvider,
    remote: {
      endpoint: response.remoteEndpoint ?? "",
      model: response.remoteModel ?? "",
      apiKey: "",
    },
    ollama: {
      endpoint: response.ollamaEndpoint ?? "",
      model: response.ollamaModel ?? "",
    },
    remoteApiKeyConfigured: response.remoteApiKeyConfigured,
    ollamaApiKeyConfigured: response.ollamaApiKeyConfigured,
  };
}

function isEqual<T>(a: T, b: T): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

export function isValidHttpUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

/**
 * Mesma validação exigida pelo backend antes de chamar o provedor (ver
 * AiCurriculumInterpreter.validateConfiguration): evita persistir um
 * endpoint/modelo/chave incompletos ou com erro de digitação (ex.: colar
 * "POST http://..." junto com a URL) que só quebrariam a extração depois.
 */
function isAiDraftValid(draft: AiPreferencesDraft): boolean {
  const needsRemote =
    draft.provider === "remote" ||
    draft.provider === "remote-with-ollama-fallback";
  const needsOllama =
    draft.provider === "remote-with-ollama-fallback" ||
    draft.provider === "ollama-only";

  if (needsRemote) {
    const hasApiKey =
      draft.remote.apiKey.trim().length > 0 || draft.remoteApiKeyConfigured;
    if (!isValidHttpUrl(draft.remote.endpoint)) return false;
    if (draft.remote.model.trim().length === 0) return false;
    if (!hasApiKey) return false;
  }

  if (needsOllama) {
    if (!isValidHttpUrl(draft.ollama.endpoint)) return false;
    if (draft.ollama.model.trim().length === 0) return false;
  }

  return true;
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
              {
                provider: state.aiDraft.provider,
                remoteEndpoint: state.aiDraft.remote.endpoint,
                remoteModel: state.aiDraft.remote.model,
                remoteApiKey:
                  state.aiDraft.remote.apiKey.trim() === ""
                    ? undefined
                    : state.aiDraft.remote.apiKey,
                ollamaEndpoint: state.aiDraft.ollama.endpoint,
                ollamaModel: state.aiDraft.ollama.model,
              },
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
