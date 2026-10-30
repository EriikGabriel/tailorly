import type {
  AiPreferencesResponse,
  UpdateAiPreferencesRequest,
} from "@lib/api/generated/model";
import {
  defaultInterpretationSettings,
  type InterpretationProvider,
} from "../constants/ai-providers";
import type { AiPreferencesDraft } from "../types/profile-settings";

/** Rascunho inicial de IA, com os valores padrão do formulário. */
export function defaultAiPreferencesDraft(): AiPreferencesDraft {
  return {
    provider: defaultInterpretationSettings.provider,
    remote: {
      endpoint: defaultInterpretationSettings.remote.endpoint,
      model: defaultInterpretationSettings.remote.model,
      apiKey: "",
    },
    ollama: {
      endpoint: defaultInterpretationSettings.ollama.endpoint,
      model: defaultInterpretationSettings.ollama.model,
    },
    remoteApiKeyConfigured: false,
    ollamaApiKeyConfigured: false,
  };
}

/** Converte a resposta da API em rascunho; a chave de API nunca volta em texto puro. */
export function aiDraftFromResponse(
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
export function isAiDraftValid(draft: AiPreferencesDraft): boolean {
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

/** Corpo do `PUT /v1/ai-preferences` a partir do rascunho; chave vazia = manter a já configurada. */
export function aiPreferencesRequestFromDraft(
  draft: AiPreferencesDraft,
): UpdateAiPreferencesRequest {
  return {
    provider: draft.provider,
    remoteEndpoint: draft.remote.endpoint,
    remoteModel: draft.remote.model,
    remoteApiKey:
      draft.remote.apiKey.trim() === "" ? undefined : draft.remote.apiKey,
    ollamaEndpoint: draft.ollama.endpoint,
    ollamaModel: draft.ollama.model,
  };
}
