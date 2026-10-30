import { Button } from "@animate/buttons/button";
import type { CvGenerationResponse } from "@lib/api/generated/model";
import { Download, FileText } from "@react-zero-ui/icon-sprite";
import type { ViewMode } from "../../-types/view-mode";
import { ViewModeTabs } from "./view-mode-tabs";

type StageHeaderProps = {
  generation: CvGenerationResponse | undefined;
  /** Ground Truth e vaga informados: a geração já pode ser disparada. */
  ready: boolean;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  onDownload: (format: "pdf" | "markdown") => void;
};

function subtitle(
  generation: CvGenerationResponse | undefined,
  ready: boolean,
) {
  if (generation) {
    return `Gerado do Ground Truth v${generation.groundTruth?.snapshotNumber} e da vaga v${generation.job?.versionNumber}`;
  }
  return ready
    ? "Pronto para gerar • revise antes de baixar"
    : "Preencha o Ground Truth e a vaga alvo para liberar a geração";
}

/** Cabeçalho do palco: título, origem da geração, modo de visualização e downloads. */
export function StageHeader({
  generation,
  ready,
  viewMode,
  onViewModeChange,
  onDownload,
}: StageHeaderProps) {
  const generationId = generation?.id;

  return (
    <div
      className={`flex flex-wrap items-center justify-between gap-3 bg-card ${
        viewMode !== "visual"
          ? "rounded-lg px-3 py-2 shadow-xs"
          : "rounded-xl p-4 shadow-xs"
      }`}
    >
      <div className="flex min-w-0 flex-col gap-1.5">
        <h2 className="text-base font-semibold leading-6 text-on-surface">
          Currículo Otimizado
        </h2>
        <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
          {subtitle(generation, ready)}
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <ViewModeTabs value={viewMode} onChange={onViewModeChange} />
        <Button
          type="button"
          variant="outline"
          onClick={() => onDownload("markdown")}
          disabled={!generationId}
          className="h-9.5 gap-1.5 rounded-lg  px-3 text-xs font-semibold text-on-surface disabled:opacity-40"
        >
          <FileText className="size-3.5" aria-hidden="true" />
          Markdown
        </Button>
        <Button
          type="button"
          onClick={() => onDownload("pdf")}
          disabled={!generationId}
          title={generationId ? undefined : "Disponível após gerar o currículo"}
          className="h-9.5 gap-1.5 rounded-lg bg-primary-900 px-3 text-xs font-semibold text-primary-foreground disabled:opacity-40"
        >
          <Download className="size-3.5" aria-hidden="true" />
          Baixar PDF
        </Button>
      </div>
    </div>
  );
}
