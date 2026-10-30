import type { JobPostingVersionResponse } from "@lib/api/generated/model";
import { RefreshCw } from "@react-zero-ui/icon-sprite";
import { ANALYSIS_STATUS_MESSAGES } from "../../-constants/job-labels";

/** Aviso quando a análise por IA não concluiu, com "Tentar de novo" para falhas. */
export function AnalysisStatus({
  version,
  onRetry,
  retrying,
}: {
  version: JobPostingVersionResponse;
  onRetry: () => void;
  retrying: boolean;
}) {
  const status = version.analysisStatus;
  if (status === "completed") return null;

  return (
    <div className="flex items-start justify-between gap-2 rounded-lg bg-surface-container p-2.5 text-[13px] leading-5 text-on-surface-variant">
      <span>
        {ANALYSIS_STATUS_MESSAGES[status ?? ""] ??
          "Análise por IA indisponível."}
      </span>
      {status === "failed" && (
        <button
          type="button"
          onClick={onRetry}
          disabled={retrying}
          className="inline-flex shrink-0 items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container-high disabled:opacity-50"
        >
          <RefreshCw
            className={`size-3 ${retrying ? "animate-spin" : ""}`}
            aria-hidden="true"
          />
          Tentar de novo
        </button>
      )}
    </div>
  );
}
