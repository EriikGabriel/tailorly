import { ApiClientError } from "@lib/api/client";
import { useBaseDocumentExtraction } from "@queries/base-document";
import { useBaseReview } from "@queries/base-review";
import { useGenerateCv } from "@queries/cv-generation";
import { useAnalyzeJobPosting } from "@queries/job-posting";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useQuickReviewStore } from "@stores/quick-review-store";
import { useNavigate } from "@tanstack/react-router";
import {
  extractionState,
  generationBlockReason,
} from "@/utils/generation-gate";
import { hasJobTarget } from "@/utils/job-draft";
import { countQuickReviewIssues } from "@/utils/job-fact-match";
import { checkJobUrl } from "@/utils/job-url";

/** Até quantas pendências rápidas o diálogo abre; acima disso a auditoria completa é necessária. */
const MAX_DIALOG_ISSUES = 6;

/**
 * Fluxo "gerar" compartilhado pela home e pelo gerador: valida o link, lê e analisa a vaga no
 * servidor e decide o próximo passo pelo volume de pendências que a validação rápida mostraria:
 * nenhuma pendência gera direto, até {@link MAX_DIALOG_ISSUES} abre o diálogo, mais que isso manda
 * para a auditoria completa do Currículo Base sem abrir o diálogo.
 */
export function useJobTarget() {
  const { mode, jobUrl, jobText, saveDraft } = useJobDraftStore();
  const baseCv = useBaseCvStore((state) => state.file);
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const setReviewOpen = useQuickReviewStore((state) => state.setOpen);
  const analyze = useAnalyzeJobPosting();
  const generateCv = useGenerateCv();
  const review = useBaseReview(baseVersionId ?? undefined);
  const navigate = useNavigate();

  const extraction = useBaseDocumentExtraction(baseVersionId ?? undefined);
  const extractionStatus = extractionState({
    hasFile: Boolean(baseCv),
    versionId: baseVersionId,
    queryFailed: extraction.isError,
    runStatus: extraction.data?.[0]?.status,
  });

  const urlCheck = checkJobUrl(jobUrl);
  const hasJob = hasJobTarget(mode, jobUrl, jobText);
  // O servidor recusaria um link inválido; bloquear aqui evita a ida e volta.
  const blocked = mode === "url" && urlCheck.status === "invalid";
  // Gerar exige o Currículo Base extraído com sucesso (nada em fila ou processando) e a vaga válida.
  const blockReason = generationBlockReason({
    extraction: extractionStatus,
    mode,
    urlCheck,
    hasJobText: jobText.trim().length > 0,
  });
  const canGenerate = blockReason === null;

  async function generate() {
    if (!canGenerate || analyze.isPending) return;
    let analysis: Awaited<ReturnType<typeof analyze.mutateAsync>>["analysis"];
    try {
      const result = await analyze.mutateAsync(
        mode === "url" ? { mode, url: jobUrl } : { mode, text: jobText },
      );
      analysis = result.analysis;
    } catch {
      // O motivo fica em `analyze.error` e a validação não abre sem a vaga lida.
      return;
    }
    saveDraft();

    // Sem os dados de pendências carregados não dá para decidir com segurança; o diálogo é o
    // destino padrão e deixa a pessoa seguir dali, em vez de pular a revisão às cegas.
    if (review.issues.isLoading || review.records.isLoading) {
      setReviewOpen(true);
      return;
    }

    const pendingFacts = (review.issues.data ?? [])
      .filter((issue) => issue.status === "PENDING" && issue.fact)
      .map((issue) => issue.fact as NonNullable<typeof issue.fact>);
    const quickIssueCount = countQuickReviewIssues(
      analysis,
      review.records.data ?? [],
      pendingFacts,
    );

    if (quickIssueCount > MAX_DIALOG_ISSUES) {
      navigate({ to: "/base" });
      return;
    }
    if (quickIssueCount === 0) {
      try {
        await generateCv.mutateAsync();
      } catch (cause) {
        // Nenhuma pendência é da vaga, mas o Ground Truth nunca foi aprovado (ou tem outras
        // pendências sem relação com ela, que também bloqueiam a aprovação): só a auditoria
        // completa resolve. Esse é o único 400 possível neste fluxo.
        if (cause instanceof ApiClientError && cause.status === 400) {
          generateCv.reset();
          navigate({ to: "/base" });
          return;
        }
        // Outras falhas (cota, vaga, armazenamento) ficam em `generateCv.error`.
        return;
      }
      navigate({ to: "/generator" });
      return;
    }
    setReviewOpen(true);
  }

  return {
    mode,
    urlCheck,
    hasJob,
    blocked,
    canGenerate,
    hasBaseCv: Boolean(baseCv),
    extractionStatus,
    blockReason,
    generate,
    isAnalyzing: analyze.isPending || generateCv.isPending,
    error: analyze.isError
      ? analyze.error.message
      : generateCv.isError
        ? generateCv.error.message
        : null,
  };
}
