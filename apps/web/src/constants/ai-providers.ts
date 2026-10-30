// As opções, defaults e nomes de propriedade abaixo espelham
// `tailorly.interpretation.*` em services/api/src/main/resources/application.yml
// e os records RemoteInterpretationProperties/OllamaInterpretationProperties
// (`AiCurriculumInterpreter`). Hoje só a extração do Currículo Base tem IA de
// fato implementada no backend; geração sob ATS (RF03) e due diligence (RF08)
// ainda são planejadas/backlog.
export const interpretationProviderOptions = [
  {
    value: "remote-with-ollama-fallback",
    name: "Provedor remoto com fallback automático (Ollama local)",
    description:
      "Usa o provedor remoto configurado abaixo para interpretar o currículo; se ele falhar, tenta automaticamente um modelo Ollama instalado neste dispositivo.",
    recommended: true,
  },
  {
    value: "remote",
    name: "Somente provedor remoto",
    description:
      "Usa apenas o provedor remoto configurado abaixo. Se ele falhar, a extração falha e deve ser tentada novamente — sem fallback local.",
    recommended: false,
  },
  {
    value: "ollama-only",
    name: "Somente modelo local (Ollama)",
    description:
      "Interpreta o currículo inteiramente no Ollama deste dispositivo, sem nenhuma chamada a provedores remotos.",
    recommended: false,
  },
  {
    value: "rules",
    name: "Somente regras (sem IA)",
    description:
      "Extração determinística baseada em regras, sem chamadas a LLM. Sempre disponível, mesmo offline, porém com menor cobertura semântica.",
    recommended: false,
  },
] as const;

export type InterpretationProvider =
  (typeof interpretationProviderOptions)[number]["value"];

// O backend só expõe um slot de "provedor remoto" (tailorly.interpretation.remote.*),
// mas qualquer serviço compatível com o formato Chat Completions da OpenAI funciona
// ali — inclusive Anthropic, DeepSeek e OpenRouter, que mantêm camadas de
// compatibilidade OpenAI nos próprios endpoints. Os presets abaixo só pré-preenchem
// endpoint/placeholder de modelo; o valor salvo continua indo para os mesmos campos
// remote.* do backend.
export const remoteProviderVendors = [
  {
    id: "openai",
    name: "OpenAI",
    endpoint: "https://api.openai.com/v1/chat/completions",
    modelPlaceholder: "ex.: gpt-4o-mini",
  },
  {
    id: "anthropic",
    name: "Anthropic (Claude)",
    endpoint: "https://api.anthropic.com/v1/chat/completions",
    modelPlaceholder: "ex.: claude-sonnet-4-5",
  },
  {
    id: "deepseek",
    name: "DeepSeek",
    endpoint: "https://api.deepseek.com/v1/chat/completions",
    modelPlaceholder: "ex.: deepseek-chat",
  },
  {
    id: "openrouter",
    name: "OpenRouter",
    endpoint: "https://openrouter.ai/api/v1/chat/completions",
    modelPlaceholder: "ex.: anthropic/claude-3.5-sonnet",
  },
  {
    id: "custom",
    name: "Outro compatível com Chat Completions",
    endpoint: "",
    modelPlaceholder: "ex.: modelo do seu provedor",
  },
] as const;

export type RemoteProviderVendor = (typeof remoteProviderVendors)[number]["id"];

/** Valores iniciais do formulário de IA, antes de a API devolver as preferências salvas. */
export const defaultInterpretationSettings: {
  provider: InterpretationProvider;
  remote: { endpoint: string; model: string; apiKey: string };
  ollama: { endpoint: string; model: string; apiKey: string };
} = {
  provider: "remote-with-ollama-fallback",
  remote: {
    endpoint: "https://api.openai.com/v1/chat/completions",
    model: "",
    apiKey: "",
  },
  ollama: {
    endpoint: "http://127.0.0.1:11434/v1/chat/completions",
    model: "qwen3:8b",
    apiKey: "ollama",
  },
};
