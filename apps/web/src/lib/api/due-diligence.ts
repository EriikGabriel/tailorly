import { type ApiRequestOptions, apiRequest } from "./client";

// Cliente escrito à mão para `/v1/due-diligence`: o contrato ainda não foi exportado para
// `services/api/openapi/tailorly.json`. Ao rodar `pnpm run api:sync` com a API no ar, troque por
// `lib/api/generated` e remova este arquivo.

export type DiligenceFactorId =
  | "labor_issues"
  | "former_employee_reports"
  | "culture"
  | "financial_health"
  | "online_reputation"
  | "legal_proceedings"
  | "suppliers"
  | "customers"
  | "regulators"
  | "investors"
  | "partners";

export type DiligenceFactorStatus =
  | "evidence_found"
  | "no_data"
  | "source_unavailable";
export type UncertaintyLevel = "low" | "medium" | "high";
export type SourceType = "official" | "company" | "news" | "review" | "other";
export type Polarity = "positive" | "negative" | "neutral";

export interface DiligenceFinding {
  claim: string;
  sourceUrl: string;
  sourceTitle: string;
  excerpt: string;
  sourceType: SourceType;
  polarity: Polarity;
  /** Relato de pessoas: não comprova o fato. */
  unverifiedReport: boolean;
  /** Falso quando a fonte pode tratar de outra empresa de nome parecido. */
  entityConfirmed: boolean;
  retrievedAt: string;
}

export interface DiligenceFactorResult {
  factor: DiligenceFactorId;
  status: DiligenceFactorStatus;
  summary: string;
  uncertainty: { level: UncertaintyLevel; note: string };
  findings: DiligenceFinding[];
}

export interface DueDiligenceReport {
  entity: {
    name: string;
    website: string | null;
    jobPostingId: string | null;
    jobVersionNumber: number | null;
    jobTitle: string | null;
  };
  consultedAt: string;
  consultedSources: string[];
  privateSourcesConsulted: boolean;
  analyzerVersion: string;
  factors: DiligenceFactorResult[];
}

/** `running` e `failed` não têm relatório. */
export type DueDiligenceStatus = "running" | "completed" | "failed";
/** `pipeline`: disparada sozinha pela esteira depois do Ground Truth, ao ler a vaga. */
export type DueDiligenceTrigger = "manual" | "pipeline";

export interface DueDiligenceResponse {
  id: string;
  ownerId: string;
  companyName: string;
  jobPostingId: string | null;
  jobVersionNumber: number | null;
  analyzerVersion: string;
  status: DueDiligenceStatus;
  triggerKind: DueDiligenceTrigger;
  report: DueDiligenceReport | null;
  createdAt: string;
}

export interface DueDiligenceSummary {
  id: string;
  status: DueDiligenceStatus;
  triggerKind: DueDiligenceTrigger;
  companyName: string;
  jobTitle: string | null;
  jobPostingId: string | null;
  factorsWithEvidence: number;
  factorsTotal: number;
  createdAt: string;
}

export interface CreateDueDiligenceInput {
  ownerId: string;
  companyName?: string;
  jobPostingId?: string;
}

export const dueDiligenceCreate = (
  input: CreateDueDiligenceInput,
  options?: ApiRequestOptions,
) =>
  apiRequest<DueDiligenceResponse>("/v1/due-diligence", {
    ...options,
    method: "POST",
    headers: { "Content-Type": "application/json", ...options?.headers },
    body: JSON.stringify(input),
  });

export const dueDiligenceFindByOwner = (
  ownerId: string,
  options?: ApiRequestOptions,
) =>
  apiRequest<DueDiligenceSummary[]>(
    `/v1/due-diligence/owner/${encodeURIComponent(ownerId)}`,
    options,
  );

export const dueDiligenceFindById = (id: string, options?: ApiRequestOptions) =>
  apiRequest<DueDiligenceResponse>(
    `/v1/due-diligence/${encodeURIComponent(id)}`,
    options,
  );

export const dueDiligenceDelete = (id: string, options?: ApiRequestOptions) =>
  apiRequest<void>(`/v1/due-diligence/${encodeURIComponent(id)}`, {
    ...options,
    method: "DELETE",
  });
