// As opções, defaults e nomes de propriedade abaixo espelham
// `tailorly.interpretation.*` em services/api/src/main/resources/application.yml
// e os records RemoteInterpretationProperties/OllamaInterpretationProperties
// (`AiCurriculumInterpreter`). Este provedor atende todas as etapas com IA
// (extração, análise da vaga, esteira de ATS, enriquecimento e evidências);
// due diligence de empresas (RF08) também usa este provedor.
export const interpretationProviderOptions = [
  {
    value: "remote-with-ollama-fallback",
    name: "Provedor remoto com fallback automático (Ollama local)",
    description:
      "Usa o provedor remoto configurado abaixo em todas as etapas com IA; se ele falhar, tenta automaticamente um modelo Ollama instalado neste dispositivo.",
    recommended: true,
  },
  {
    value: "remote",
    name: "Somente provedor remoto",
    description:
      "Usa apenas o provedor remoto configurado abaixo em todas as etapas com IA. Se ele falhar, a extração falha e deve ser tentada novamente; na esteira de ATS, as regras assumem — sem fallback local.",
    recommended: false,
  },
  {
    value: "claude-cli",
    name: "Claude Code local (sua assinatura)",
    description:
      "Usa o Claude Code logado neste computador, consumindo os limites da sua assinatura Claude em vez de cobrar por requisição. Só aparece quando o servidor local o habilita.",
    recommended: false,
    localOnly: true,
  },
  {
    value: "codex-cli",
    name: "Codex local (sua conta ChatGPT)",
    description:
      "Usa o Codex CLI logado neste computador, consumindo os limites da sua conta ChatGPT em vez de cobrar por requisição. Só aparece quando o servidor local o habilita.",
    recommended: false,
    localOnly: true,
  },
  {
    value: "ollama-only",
    name: "Somente modelo local (Ollama)",
    description:
      "Todas as etapas com IA rodam no Ollama deste dispositivo, sem nenhuma chamada a provedores remotos.",
    recommended: false,
  },
  {
    value: "rules",
    name: "Somente regras (sem IA)",
    description:
      "Etapas de IA usam regras determinísticas, sem chamadas a LLM. Sempre disponível, mesmo offline, porém com menor cobertura semântica.",
    recommended: false,
  },
] as const;

export type InterpretationProvider =
  (typeof interpretationProviderOptions)[number]["value"];

/** Níveis de esforço dos CLIs locais, do mais rápido ao mais caprichado. */
export const localCliEffortOptions = [
  { value: "low", label: "Baixo — mais rápido" },
  { value: "medium", label: "Médio" },
  { value: "high", label: "Alto" },
  { value: "xhigh", label: "Muito alto" },
  { value: "max", label: "Máximo — mais lento" },
] as const;

/** Nomes das etapas com IA (chaves de `LocalCliEfforts.TASKS` no backend). */
export const localCliTaskLabels: Record<
  string,
  { name: string; hint: string }
> = {
  interpretation: {
    name: "Extração do Currículo Base",
    hint: "Fatos do Ground Truth; erros aqui contaminam o resto.",
  },
  "job-analysis": {
    name: "Análise da vaga",
    hint: "Anota requisitos já separados por seção.",
  },
  "due-diligence": {
    name: "Due diligence da empresa",
    hint: "Fatores a partir de fontes públicas.",
  },
  "supplementary-evidence": {
    name: "Evidências complementares",
    hint: "Afirmações com trecho citado dos links.",
  },
  "content-enrichment": {
    name: "Adaptação do conteúdo à vaga",
    hint: "Reescreve sem inventar fatos.",
  },
  "ats-review": {
    name: "Revisão de ATS",
    hint: "As regras já cobrem o básico.",
  },
  "resume-layout-plan": {
    name: "Plano de layout do currículo",
    hint: "Decisão simples sobre dados prontos.",
  },
  default: {
    name: "Demais etapas",
    hint: "Etapas novas sem esforço próprio.",
  },
};

/** Diferenças entre os CLIs locais mostradas no perfil. */
export const localCliMeta = {
  "claude-cli": {
    title: "Claude Code local — sua assinatura",
    description:
      "Cada etapa roda o Claude Code logado neste computador, sem ferramentas, e consome os limites da sua assinatura Claude. É mais lento que uma API (um processo por vez).",
    loginCommand: "claude → /login",
    enableVariable: "TAILORLY_CLAUDE_CLI_ENABLED",
    modelSuggestions: ["sonnet", "opus", "haiku"],
    allowEmptyModel: false,
    modelHint: "Alias (sonnet, opus, haiku) ou nome completo.",
  },
  "codex-cli": {
    title: "Codex local — sua conta ChatGPT",
    description:
      "Cada etapa roda o Codex CLI logado neste computador, com sandbox somente leitura e sem shell, navegador ou plugins, e consome os limites da sua conta ChatGPT. É mais lento que uma API (um processo por vez).",
    loginCommand: "codex login",
    enableVariable: "TAILORLY_CODEX_CLI_ENABLED",
    modelSuggestions: [],
    allowEmptyModel: true,
    modelHint:
      "Vazio usa o modelo padrão do Codex. O esforço Máximo vira Muito alto no Codex.",
  },
} as const;

/** O modelo vira argumento do CLI: só nomes simples, sem espaço nem hífen inicial. */
export const LOCAL_CLI_MODEL_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._:/[\]-]*$/;
export const LOCAL_CLI_TIMEOUT_RANGE = { min: 10, max: 1800 } as const;

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
