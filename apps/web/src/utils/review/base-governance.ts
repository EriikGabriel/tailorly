import type { ApiReviewIssue, FactRecord } from "@@types/base-review";

/** Contagens dos fatos e pendências da versão do Currículo Base, para os cartões de governança. */
export type GovernanceSummaryData = {
  /** Fatos que contam: tudo que não foi omitido pela pessoa. */
  considered: number;
  /** Fatos confirmados ou corrigidos pela pessoa. */
  reviewed: number;
  /** Fatos ainda pendentes; ficam fora do currículo até serem revisados. */
  pending: number;
  /** Fatos considerados com ao menos um trecho de origem no documento. */
  withEvidence: number;
  /** Pendências obrigatórias que bloqueiam a aprovação. */
  blocking: number;
  /** Índice de 0 a 100 (fatos revisados sobre os considerados); nulo sem fatos. */
  reliability: number | null;
};

export function summarizeGovernance(
  records: readonly FactRecord[],
  issues: readonly ApiReviewIssue[],
): GovernanceSummaryData {
  const facts = records.flatMap((record) => record.facts);
  const considered = facts.filter((fact) => fact.reviewState !== "omitido");
  const reviewed = considered.filter(
    (fact) =>
      fact.reviewState === "confirmado" || fact.reviewState === "corrigido",
  ).length;
  return {
    considered: considered.length,
    reviewed,
    pending: considered.length - reviewed,
    withEvidence: considered.filter((fact) => fact.evidence.length > 0).length,
    blocking: issues.filter(
      (issue) => issue.blocking && issue.status === "PENDING",
    ).length,
    reliability:
      considered.length === 0
        ? null
        : Math.round((reviewed / considered.length) * 100),
  };
}

export type IntegrityState = {
  title: string;
  detail: string;
  /** `ok` pronto/aprovado, `warning` exige ação, `idle` ainda sem dados. */
  tone: "ok" | "warning" | "idle";
};

/** Estado do protocolo de integridade: o que falta para os fatos poderem virar Ground Truth. */
export function integrityState(
  summary: GovernanceSummaryData,
  approved: boolean,
  loading: boolean,
): IntegrityState {
  if (loading) {
    return { title: "Verificando…", detail: "Lendo os fatos", tone: "idle" };
  }
  if (summary.considered === 0) {
    return {
      title: "Aguardando revisão",
      detail: "Nenhum fato extraído ainda",
      tone: "idle",
    };
  }
  if (summary.blocking > 0) {
    return {
      title: `${summary.blocking} pendência${summary.blocking === 1 ? "" : "s"} obrigatória${summary.blocking === 1 ? "" : "s"}`,
      detail: "Resolva antes de aprovar o currículo base",
      tone: "warning",
    };
  }
  if (summary.pending > 0) {
    return {
      title: "Revisão em andamento",
      detail: `${summary.pending} fato${summary.pending === 1 ? "" : "s"} pendente${summary.pending === 1 ? "" : "s"} ficam fora do currículo`,
      tone: "warning",
    };
  }
  return approved
    ? {
        title: "Aprovado como Ground Truth",
        detail: `${summary.reviewed} fatos confirmados`,
        tone: "ok",
      }
    : {
        title: "Pronto para aprovar",
        detail: `${summary.reviewed} fatos confirmados, sem pendências`,
        tone: "ok",
      };
}
