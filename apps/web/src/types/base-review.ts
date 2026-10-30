/**
 * Tipos de domínio da revisão do Currículo Base, como a API os devolve.
 * `ApiReviewIssue` é a pendência crua do servidor; `ReviewIssue` (em `review.ts`)
 * é a pendência já montada para a interface.
 */

export type FactEvidence = {
  id: string;
  extractedPageId: string;
  sourceExcerpt: string;
  sourceLocator: unknown;
};

export type BaseFact = {
  id: string;
  recordId: string;
  fieldKey: string;
  extractedValue: unknown;
  reviewedValue: unknown;
  origin: string;
  reviewState: "pendente" | "confirmado" | "corrigido" | "omitido";
  evidence: FactEvidence[];
};

export type FactRecord = {
  id: string;
  baseVersionId: string;
  category: string;
  displayOrder: number;
  facts: BaseFact[];
};

export type ApiReviewIssue = {
  id: string;
  baseVersionId: string;
  factId: string | null;
  category: string;
  reason: string;
  blocking: boolean;
  status: string;
  questionAndChoices: Record<string, unknown> | null;
  fact: BaseFact | null;
};

export type GroundTruthSnapshot = {
  id: string;
  ownerId: string;
  baseVersionId: string;
  snapshotNumber: number;
  approvedReviewRevision: number;
  contentHash: string;
  approvedAt: string;
  schemaVersion: number;
  active: boolean;
  items: {
    id: string;
    category: string;
    fieldKey: string;
    approvedValue: unknown;
  }[];
};
