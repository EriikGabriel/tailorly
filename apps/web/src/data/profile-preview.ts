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
