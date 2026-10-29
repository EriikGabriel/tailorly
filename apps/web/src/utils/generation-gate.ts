import type { JobUrlCheck } from "./job-url";

export type ExtractionState =
  | "missing"
  | "uploading"
  | "unknown"
  | "waiting"
  | "processing"
  | "failed"
  | "ready";

/**
 * Situação do Currículo Base para gerar: só `ready` (execução mais recente concluída com sucesso)
 * libera a geração; sem arquivo, enviando, na fila, em processamento ou com falha, não.
 */
export function extractionState(input: {
  hasFile: boolean;
  versionId: string | null;
  queryFailed: boolean;
  /** Status da execução mais recente; indefinido enquanto nenhuma execução existe. */
  runStatus: string | undefined;
}): ExtractionState {
  if (!input.hasFile) return "missing";
  if (!input.versionId) return "uploading";
  if (input.queryFailed) return "unknown";
  switch (input.runStatus) {
    case undefined:
      return "waiting";
    case "QUEUED":
    case "PROCESSING":
      return "processing";
    case "COMPLETED":
      return "ready";
    default:
      return "failed";
  }
}

const extractionReasons: Record<Exclude<ExtractionState, "ready">, string> = {
  missing: "Envie o Currículo Base para liberar a geração.",
  uploading: "Aguarde o envio do Currículo Base terminar.",
  unknown:
    "Não foi possível consultar a extração do Currículo Base. Recarregue a página.",
  waiting: "Aguarde o início da extração do Currículo Base.",
  processing: "Aguarde a extração do Currículo Base terminar.",
  failed: "A extração do Currículo Base falhou. Envie o PDF novamente.",
};

/** Motivo para a geração estar bloqueada, ou nulo quando está liberada. */
export function generationBlockReason(input: {
  extraction: ExtractionState;
  mode: "url" | "text";
  urlCheck: JobUrlCheck;
  hasJobText: boolean;
}): string | null {
  if (input.extraction !== "ready") return extractionReasons[input.extraction];

  if (input.mode === "url") {
    switch (input.urlCheck.status) {
      case "empty":
        return "Informe o link da vaga para liberar a geração.";
      case "invalid":
      case "warning":
        return input.urlCheck.message;
      case "valid":
        return null;
    }
  }
  return input.hasJobText
    ? null
    : "Informe o texto da vaga para liberar a geração.";
}
