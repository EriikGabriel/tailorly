// Dados de demonstração para a prévia de Perfil & Configurações.
// Representam um executivo fictício para ilustrar o layout; nunca são
// extraídos de um Currículo Base real nem persistidos como fatos de conta.
// A integração com a API de perfil/preferências ainda está planejada.

export const previewExecutiveProfile = {
  name: "Lucas Mendonça de Alvim",
  role: "Engenheiro de Software Sênior & Tech Lead",
  location: "São Paulo, SP — Brasil (Remoto / Híbrido)",
  plan: "Plano Pro Executive",
  linkedin: "linkedin/in/lucasalvim",
  github: "github/lucasalvim",
  stats: { resumes: 18, atsScore: 94.2, exports: 12 },
} as const;

export const previewGroundTruthStatus = {
  fileName: "Lucas_CV_Principal_2025.pdf",
  updatedAtLabel: "Atualizado em 14 Jan 2025",
  sizeLabel: "284 KB",
  yearsOfExperience: 8,
  validatedSkills: 14,
  factualFidelity: 94,
  fidelityRate: 98.4,
  approvedSections: 4,
  totalSections: 4,
} as const;

export const previewGroundTruthFacts = {
  executiveSummary:
    "Engenheiro de Software Sênior e Tech Lead com mais de 8 anos de experiência em sistemas distribuídos de alta escala, microsserviços em Go/Node.js e arquiteturas de nuvem AWS. Liderança técnica de squads multidisciplinares focadas em confiabilidade, redução de latência crítica e integração contínua.",
  timelineRangeLabel: "2016 — Presente",
  totalPositions: 4,
  positions: [
    {
      role: "Tech Lead & Principal Architect",
      period: "2022 — Atual",
      company: "FinTech Solutions Brasil",
      highlight: "5 conquistas quantificadas • 40M+ transações/dia",
    },
    {
      role: "Senior Software Engineer",
      period: "2019 — 2022",
      company: "HealthTech Corp LatAm",
      highlight: "3 projetos de migração cloud • Microsserviços",
    },
  ],
  education: "Bacharelado em Engenharia de Computação — USP (2012-2016)",
  certifications: [
    "AWS Certified Solutions Architect – Professional (SAP-C02)",
    "Certified Kubernetes Administrator (CKA)",
  ],
} as const;

export const previewIdentityFields: {
  fullName: string;
  email: string;
  phone: string;
  headline: string;
  location: string;
  linkedinUrl: string;
  githubUrl: string;
} = {
  fullName: "Lucas Mendonça de Alvim",
  email: "lucas.alvim@engcareer.com.br",
  phone: "+55 (11) 98765-4321",
  headline: "Engenheiro de Software Sênior & Tech Lead",
  location:
    "São Paulo, SP - Brasil (Disponível para trabalho Remoto Global ou Híbrido)",
  linkedinUrl: "https://linkedin.com/in/lucasalvim",
  githubUrl: "https://github.com/lucasalvim",
};

export const previewSecondBrainConnections: {
  notion: {
    name: string;
    detail: string;
    workspace: string;
    description: string;
    connected: boolean;
    archiveGenerated: boolean;
    useForBullets: boolean;
  };
  obsidian: {
    name: string;
    detail: string;
    vaultPath: string;
    description: string;
    connected: boolean;
    exportMarkdown: boolean;
    linkJobTags: boolean;
  };
} = {
  notion: {
    name: "Notion Workspace",
    detail: "Carreira & Vagas (18 notas sync)",
    workspace: "Lucas Personal OS",
    description:
      "Sincroniza anotações de retrospectivas técnicas e métricas quantificáveis de squads passadas.",
    connected: true,
    archiveGenerated: true,
    useForBullets: true,
  },
  obsidian: {
    name: "Obsidian Vault",
    detail: "/Tailorly/Curriculos_Gerados/",
    vaultPath: "~/Knowledge/Vault/Tailorly",
    description:
      "Espelha saídas em Markdown estruturado para visualização em grafo de conhecimento.",
    connected: true,
    exportMarkdown: true,
    linkJobTags: false,
  },
};

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
      "Usa o provedor remoto configurado abaixo para interpretar o currículo; se ele falhar, tenta automaticamente um modelo Ollama instalado neste dispositivo (RNF07).",
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

export const previewInterpretationSettings: {
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

export const typographyOptions = [
  {
    value: "harvard-precision",
    label: "Harvard Precision Tech (A4 Executivo Editorial)",
  },
  { value: "ats-classic", label: "ATS Classic Sans (Letter Compacto)" },
  { value: "editorial-serif", label: "Editorial Serif (A4 Narrativo)" },
] as const;

export const languageOptions = [
  { value: "auto", label: "Detectar automaticamente da vaga (PT-BR / EN)" },
  { value: "pt-br", label: "Português (Brasil)" },
  { value: "en", label: "Inglês (EN-US)" },
] as const;
