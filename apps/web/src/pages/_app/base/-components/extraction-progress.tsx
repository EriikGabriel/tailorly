import {
  AiStageLabel,
  type OrbState,
} from "@components/ai-progress/ai-stage-label";
import { formatElapsed, useElapsedSeconds } from "@hooks/use-elapsed";
import { useBaseDocumentExtraction } from "@queries/base-document";
import { extractionFailureMessage } from "@utils/extraction/extraction-failures";

const STAGES: Record<string, { label: string; orb: OrbState }> = {
  QUEUED: { label: "Na fila de processamento", orb: "breathing" },
  READING_FILE: { label: "Abrindo o PDF", orb: "searching" },
  EXTRACTING_TEXT: {
    label: "Extraindo o texto (OCR quando preciso)",
    orb: "working",
  },
  SAVING_PAGES: { label: "Guardando o texto das páginas", orb: "weaving" },
  INTERPRETING: {
    label: "A IA separa os fatos do currículo",
    orb: "composing",
  },
  SAVING_FACTS: { label: "Registrando os fatos para revisão", orb: "solving" },
};

/** Andamento da extração do Currículo Base numa linha discreta: etapa atual e tempo decorrido. */
export function ExtractionProgress({ versionId }: { versionId?: string }) {
  const extraction = useBaseDocumentExtraction(versionId);
  const run = extraction.data?.[0];
  const running =
    !run || run.status === "QUEUED" || run.status === "PROCESSING";
  const elapsed = useElapsedSeconds(
    running,
    run?.startedAt ? Date.parse(run.startedAt) : null,
  );

  if (!versionId) return null;

  if (extraction.isError) {
    return (
      <p role="status" className="text-xs text-on-surface-variant">
        PDF enviado • não foi possível consultar o processamento.
      </p>
    );
  }

  if (run?.status === "FAILED") {
    return (
      <p role="alert" className="text-xs text-error">
        {extractionFailureMessage(run.errorCode)}
      </p>
    );
  }

  if (run?.status === "COMPLETED") {
    return (
      <p role="status" className="text-xs text-on-tertiary-container">
        Extração concluída
      </p>
    );
  }

  const stage =
    STAGES[run?.stage ?? (run ? "READING_FILE" : "QUEUED")] ??
    STAGES.READING_FILE;
  return (
    <div className="inline-flex max-w-full items-center gap-3 rounded-full bg-surface-container px-3 py-1.5 text-xs font-medium text-primary-950">
      <AiStageLabel
        orb={stage.orb}
        label={stage.label}
        theme="light"
        title={run?.stageDetail ?? undefined}
      />
      <span className="shrink-0 tabular-nums text-on-surface-variant">
        {formatElapsed(elapsed)}
      </span>
    </div>
  );
}
