import { useBaseDocumentExtraction } from "@queries/base-document";

export function ExtractionProgress({ versionId }: { versionId?: string }) {
  const extraction = useBaseDocumentExtraction(versionId);
  const run = extraction.data?.[0];

  if (!versionId) return null;

  if (extraction.isError) {
    return (
      <p role="status" className="text-xs text-on-surface-variant">
        PDF enviado • não foi possível consultar o processamento.
      </p>
    );
  }

  if (!run) {
    return (
      <p role="status" className="text-xs text-on-surface-variant">
        PDF enviado • aguardando início da extração.
      </p>
    );
  }

  const labels: Record<string, string> = {
    QUEUED: "Extração na fila",
    PROCESSING: "Extraindo texto das páginas",
    COMPLETED: "Extração concluída",
    FAILED: run.errorCode
      ? `Falha na extração (${run.errorCode})`
      : "Falha na extração",
  };
  const active = ["QUEUED", "PROCESSING"].includes(run.status);

  return (
    <div aria-live="polite" className="space-y-1.5">
      <p role="status" className="text-xs text-on-surface-variant">
        {labels[run.status] ?? "Status da extração indisponível"}
      </p>
      {active && (
        <div
          role="progressbar"
          aria-label={labels[run.status]}
          className="h-1.5 overflow-hidden rounded-full bg-surface-container-highest"
        >
          <div className="h-full w-1/3 animate-pulse rounded-full bg-primary-700" />
        </div>
      )}
    </div>
  );
}
