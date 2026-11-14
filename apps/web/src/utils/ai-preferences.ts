import type {
  AiPreferencesDraft,
  LocalCliDraft,
  LocalCliProvider,
  LocalCliServerInfo,
} from "@@types/profile-settings";
import type {
  AiPreferencesResponse,
  LocalCliPreferencesRequest,
  LocalCliPreferencesResponse,
  UpdateAiPreferencesRequest,
} from "@api/generated/model";
import {
  defaultInterpretationSettings,
  type InterpretationProvider,
  LOCAL_CLI_MODEL_PATTERN,
  LOCAL_CLI_TIMEOUT_RANGE,
  localCliMeta,
} from "@constants/ai-providers";

export const LOCAL_CLI_PROVIDERS: readonly LocalCliProvider[] = [
  "claude-cli",
  "codex-cli",
];

export function isLocalCliProvider(
  provider: string,
): provider is LocalCliProvider {
  return (LOCAL_CLI_PROVIDERS as readonly string[]).includes(provider);
}

const emptyLocalCli = (provider: LocalCliProvider): LocalCliDraft => ({
  model: provider === "claude-cli" ? "sonnet" : "",
  timeoutSeconds: 180,
  efforts: {},
});

const unavailable = (provider: LocalCliProvider): LocalCliServerInfo => ({
  available: false,
  defaults: emptyLocalCli(provider),
});

function localCliFromResponse(
  provider: LocalCliProvider,
  response: LocalCliPreferencesResponse | undefined,
): { draft: LocalCliDraft; server: LocalCliServerInfo } {
  // Backend anterior aos CLIs locais: sem o bloco, o provider só fica indisponível.
  if (!response)
    return { draft: emptyLocalCli(provider), server: unavailable(provider) };
  return {
    draft: {
      model: response.model ?? "",
      timeoutSeconds: response.timeoutSeconds,
      efforts: { ...(response.efforts ?? {}) },
    },
    server: {
      available: response.available,
      defaults: {
        model: response.serverModel ?? "",
        timeoutSeconds: response.serverTimeoutSeconds,
        efforts: { ...(response.serverEfforts ?? {}) },
      },
    },
  };
}

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
    localCli: {
      "claude-cli": emptyLocalCli("claude-cli"),
      "codex-cli": emptyLocalCli("codex-cli"),
    },
    localCliServer: {
      "claude-cli": unavailable("claude-cli"),
      "codex-cli": unavailable("codex-cli"),
    },
    localCliTasks: [],
  };
}

/** Converte a resposta da API em rascunho; a chave de API nunca volta em texto puro. */
export function aiDraftFromResponse(
  response: AiPreferencesResponse,
): AiPreferencesDraft {
  const claude = localCliFromResponse("claude-cli", response.claudeCli);
  const codex = localCliFromResponse("codex-cli", response.codexCli);
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
    localCli: { "claude-cli": claude.draft, "codex-cli": codex.draft },
    localCliServer: { "claude-cli": claude.server, "codex-cli": codex.server },
    localCliTasks: [...(response.localCliTasks ?? [])],
  };
}

/**
 * Mesmas regras do `LocalCliPreferencesRequest` (o modelo vira argumento do CLI). O Codex aceita
 * modelo vazio (padrão do próprio CLI); o Claude Code precisa de um.
 */
export function isLocalCliDraftValid(
  provider: LocalCliProvider,
  config: LocalCliDraft,
): boolean {
  const model = config.model.trim();
  const modelOk =
    model === ""
      ? localCliMeta[provider].allowEmptyModel
      : model.length <= 255 && LOCAL_CLI_MODEL_PATTERN.test(model);
  return (
    modelOk &&
    Number.isInteger(config.timeoutSeconds) &&
    config.timeoutSeconds >= LOCAL_CLI_TIMEOUT_RANGE.min &&
    config.timeoutSeconds <= LOCAL_CLI_TIMEOUT_RANGE.max
  );
}

function localCliRequest(
  draft: AiPreferencesDraft,
  provider: LocalCliProvider,
): LocalCliPreferencesRequest | undefined {
  // Só onde o servidor habilita o CLI; sem o bloco, o backend mantém o que já estava salvo.
  const config = draft.localCli[provider];
  if (
    !draft.localCliServer[provider].available ||
    !isLocalCliDraftValid(provider, config)
  )
    return undefined;
  return {
    model: config.model.trim(),
    timeoutSeconds: config.timeoutSeconds,
    efforts: config.efforts,
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

  if (isLocalCliProvider(draft.provider)) {
    if (!draft.localCliServer[draft.provider].available) return false;
    if (!isLocalCliDraftValid(draft.provider, draft.localCli[draft.provider]))
      return false;
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
    claudeCli: localCliRequest(draft, "claude-cli"),
    codexCli: localCliRequest(draft, "codex-cli"),
  };
}
