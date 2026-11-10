import type { InterpretationProvider } from "@constants/ai-providers";

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

/** Providers locais por CLI: rodam o CLI logado na máquina do servidor (só uso local). */
export type LocalCliProvider = "claude-cli" | "codex-cli";

/** Ajustes de um CLI local; modelo vazio usa o padrão do próprio CLI (onde ele permite). */
export interface LocalCliDraft {
  model: string;
  timeoutSeconds: number;
  /** Esforço por etapa (`default` e as chaves de `localCliTasks`). */
  efforts: Record<string, string>;
}

/** Somente leitura: o que o servidor permite e os padrões dele para um CLI local. */
export interface LocalCliServerInfo {
  available: boolean;
  defaults: LocalCliDraft;
}

export interface AiPreferencesDraft {
  provider: InterpretationProvider;
  remote: RemoteDraft;
  ollama: OllamaDraft;
  remoteApiKeyConfigured: boolean;
  ollamaApiKeyConfigured: boolean;
  localCli: Record<LocalCliProvider, LocalCliDraft>;
  /** Somente leitura: liberação e padrões do servidor, por CLI. */
  localCliServer: Record<LocalCliProvider, LocalCliServerInfo>;
  /** Somente leitura: etapas com IA, na ordem do fluxo. */
  localCliTasks: string[];
}
