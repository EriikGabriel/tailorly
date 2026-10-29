import { useBaseDocumentExtraction } from "@queries/base-document";
import { useAnalyzeJobPosting } from "@queries/job-posting";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useQuickReviewStore } from "@stores/quick-review-store";
import {
  extractionState,
  generationBlockReason,
} from "@/utils/generation-gate";
import { checkJobUrl } from "@/utils/job-url";

/**
 * Fluxo "gerar" compartilhado pela home e pelo gerador: valida o link, lê e analisa a vaga no
 * servidor e só então abre a validação dos fatos, que usa a análise para escolher o que perguntar.
 */
export function useJobTarget() {
  const { mode, jobUrl, jobText, saveDraft } = useJobDraftStore();
  const baseCv = useBaseCvStore((state) => state.file);
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const setReviewOpen = useQuickReviewStore((state) => state.setOpen);
  const analyze = useAnalyzeJobPosting();

  const extraction = useBaseDocumentExtraction(baseVersionId ?? undefined);
  const extractionStatus = extractionState({
    hasFile: Boolean(baseCv),
    versionId: baseVersionId,
    queryFailed: extraction.isError,
    runStatus: extraction.data?.[0]?.status,
  });

  const urlCheck = checkJobUrl(jobUrl);
  const hasJob =
    mode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;
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
    try {
      await analyze.mutateAsync(
        mode === "url" ? { mode, url: jobUrl } : { mode, text: jobText },
      );
    } catch {
      // O motivo fica em `analyze.error` e a validação não abre sem a vaga lida.
      return;
    }
    saveDraft();
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
    isAnalyzing: analyze.isPending,
    error: analyze.isError ? analyze.error.message : null,
  };
}
