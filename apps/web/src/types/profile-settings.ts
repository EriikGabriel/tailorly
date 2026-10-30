import type { InterpretationProvider } from "../constants/ai-providers";

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
