import type {
  DiligenceFactorId,
  DiligenceFactorStatus,
  Polarity,
  SourceType,
  UncertaintyLevel,
} from "@api/due-diligence";
import {
  Briefcase,
  Building2,
  Gavel,
  Globe,
  Handshake,
  type IconProps,
  Landmark,
  MessageSquareQuote,
  TrendingUp,
  Truck,
  Users,
  UsersRound,
} from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";

/** Os onze fatores da due diligence, na ordem do relatório; devem espelhar `DiligenceFactor` no backend. */
export type DiligenceGroupId = "people" | "business" | "compliance";

/** Agrupamento só de leitura: os onze fatores continuam todos presentes, em qualquer grupo. */
export const diligenceGroups: {
  id: DiligenceGroupId;
  label: string;
  description: string;
}[] = [
  {
    id: "people",
    label: "Pessoas & cultura",
    description: "Como é trabalhar lá, segundo fontes públicas.",
  },
  {
    id: "business",
    label: "Negócio & mercado",
    description: "Saúde financeira e relações comerciais.",
  },
  {
    id: "compliance",
    label: "Reputação & conformidade",
    description: "Imagem pública, processos e órgãos reguladores.",
  },
];

export interface DiligenceFactorMeta {
  id: DiligenceFactorId;
  label: string;
  description: string;
  group: DiligenceGroupId;
  icon: ComponentType<IconProps>;
}

export const diligenceFactors: DiligenceFactorMeta[] = [
  {
    id: "labor_issues",
    group: "people",
    icon: Briefcase,
    label: "Problemas trabalhistas",
    description: "Ações, fiscalizações e autuações trabalhistas.",
  },
  {
    id: "former_employee_reports",
    group: "people",
    icon: MessageSquareQuote,
    label: "Relatos de ex-funcionários",
    description: "Relatos em avaliações e fóruns; são relatos, não provas.",
  },
  {
    id: "culture",
    group: "people",
    icon: Users,
    label: "Cultura",
    description: "Valores, estilo de gestão, diversidade e modelo de trabalho.",
  },
  {
    id: "financial_health",
    group: "business",
    icon: Landmark,
    label: "Situação financeira",
    description: "Resultados, demissões, aportes e risco de insolvência.",
  },
  {
    id: "online_reputation",
    group: "compliance",
    icon: Globe,
    label: "Reputação online",
    description: "Imagem pública, imprensa e reclamações de consumidores.",
  },
  {
    id: "legal_proceedings",
    group: "compliance",
    icon: Gavel,
    label: "Processos legais",
    description: "Processos, multas, sanções e investigações.",
  },
  {
    id: "suppliers",
    group: "business",
    icon: Truck,
    label: "Fornecedores",
    description: "Cadeia de suprimentos e relações com fornecedores.",
  },
  {
    id: "customers",
    group: "business",
    icon: UsersRound,
    label: "Clientes",
    description: "Principais clientes e segmentos atendidos.",
  },
  {
    id: "regulators",
    group: "compliance",
    icon: Building2,
    label: "Reguladores",
    description: "Licenças, relação e penalidades com órgãos reguladores.",
  },
  {
    id: "investors",
    group: "business",
    icon: TrendingUp,
    label: "Investidores",
    description: "Acionistas, rodadas de investimento e controle.",
  },
  {
    id: "partners",
    group: "business",
    icon: Handshake,
    label: "Parceiros",
    description: "Parcerias comerciais, tecnológicas e institucionais.",
  },
];

export const factorStatusLabel: Record<DiligenceFactorStatus, string> = {
  evidence_found: "Com fontes",
  no_data: "Sem dados públicos",
  source_unavailable: "Busca indisponível",
};

export const uncertaintyLabel: Record<UncertaintyLevel, string> = {
  low: "Incerteza baixa",
  medium: "Incerteza média",
  high: "Incerteza alta",
};

export const sourceTypeLabel: Record<SourceType, string> = {
  official: "Fonte oficial",
  company: "Site da empresa",
  news: "Notícia",
  review: "Relato de pessoas",
  other: "Outra fonte",
};

export const polarityLabel: Record<Polarity, string> = {
  positive: "Sinal positivo",
  negative: "Sinal negativo",
  neutral: "Neutro",
};
