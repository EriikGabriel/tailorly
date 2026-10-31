import type { CvPreview as CvPreviewState } from "@hooks/use-cv-preview";
import {
  CircleAlert,
  CircleCheck,
  Download,
  LoaderCircle,
  Play,
  Scan,
  ZoomIn,
  ZoomOut,
} from "@react-zero-ui/icon-sprite";
import { useState } from "react";
import {
  PdfLiveViewer,
  type PreviewZoom,
  type ViewerMetrics,
} from "./pdf-live-viewer";
import { ToolbarButton } from "./typst-editor/toolbar-button";

const ZOOM_STEPS = [50, 75, 100, 125, 150, 200];
const ICON = "size-3.75";

/** Próximo passo de zoom acima ou abaixo da escala atual (a efetiva, também no modo "ajustar"). */
export function nextZoom(current: number, direction: 1 | -1): number {
  if (direction > 0)
    return (
      ZOOM_STEPS.find((step) => step > current) ??
      ZOOM_STEPS[ZOOM_STEPS.length - 1]
    );
  return (
    [...ZOOM_STEPS].reverse().find((step) => step < current) ?? ZOOM_STEPS[0]
  );
}

function statusOf(preview: CvPreviewState) {
  if (preview.isCompiling)
    return {
      icon: LoaderCircle,
      text: "Compilando...",
      tone: "text-on-surface-variant",
      spin: true,
    };
  if (preview.error)
    return {
      icon: CircleAlert,
      text: "Erro de compilação",
      tone: "text-error",
      spin: false,
    };
  if (preview.url)
    return {
      icon: CircleCheck,
      text:
        preview.durationMs === null
          ? "Atualizado"
          : `Atualizado · ${preview.durationMs} ms`,
      tone: "text-on-tertiary-container",
      spin: false,
    };
  return {
    icon: LoaderCircle,
    text: "Aguardando",
    tone: "text-on-surface-variant",
    spin: false,
  };
}

/**
 * Prévia do currículo: PDF desenhado em canvas e atualizado no lugar a cada compilação, com zoom,
 * contagem de páginas e status. O último PDF válido continua na tela se o código atual não compila.
 */
export function CvPreview({ preview }: { preview: CvPreviewState }) {
  const [zoom, setZoom] = useState<PreviewZoom>("fit");
  const [metrics, setMetrics] = useState<ViewerMetrics>({
    page: 1,
    pages: 0,
    percent: 100,
  });
  const status = statusOf(preview);
  const StatusIcon = status.icon;

  function downloadPreview() {
    if (!preview.url) return;
    const link = document.createElement("a");
    link.href = preview.url;
    link.download = "previa-curriculo.pdf";
    link.click();
  }

  return (
    <div className="relative flex h-full min-h-0 w-full flex-col overflow-hidden rounded-lg bg-card shadow-md">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/60 px-2.5 py-1.5">
        <span
          role="status"
          data-testid="preview-status"
          className={`inline-flex items-center gap-1.5 text-xs font-semibold ${status.tone}`}
        >
          <StatusIcon
            className={`size-3.5 ${status.spin ? "animate-spin" : ""}`}
            aria-hidden="true"
          />
          {status.text}
        </span>
        <span
          className="text-[11px] tabular-nums text-on-surface-variant"
          data-testid="preview-pages"
        >
          {metrics.pages > 0
            ? `Página ${metrics.page} de ${metrics.pages}`
            : ""}
        </span>
        <div className="flex items-center gap-0.5">
          <ToolbarButton
            label="Compilar agora (Ctrl+Enter)"
            onClick={preview.compileNow}
            disabled={preview.isCompiling}
          >
            <Play className={ICON} aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton
            label="Diminuir zoom"
            onClick={() => setZoom(nextZoom(metrics.percent, -1))}
            disabled={metrics.percent <= ZOOM_STEPS[0]}
          >
            <ZoomOut className={ICON} aria-hidden="true" />
          </ToolbarButton>
          <span
            className="w-9 text-center text-[11px] tabular-nums text-on-surface-variant"
            data-testid="preview-zoom"
          >
            {metrics.percent}%
          </span>
          <ToolbarButton
            label="Aumentar zoom"
            onClick={() => setZoom(nextZoom(metrics.percent, 1))}
            disabled={metrics.percent >= ZOOM_STEPS[ZOOM_STEPS.length - 1]}
          >
            <ZoomIn className={ICON} aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton
            label="Ajustar à largura"
            onClick={() => setZoom("fit")}
            active={zoom === "fit"}
          >
            <Scan className={ICON} aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton
            label="Baixar esta prévia (PDF)"
            onClick={downloadPreview}
            disabled={!preview.url}
          >
            <Download className={ICON} aria-hidden="true" />
          </ToolbarButton>
        </div>
      </div>

      {preview.url ? (
        <PdfLiveViewer url={preview.url} zoom={zoom} onMetrics={setMetrics} />
      ) : (
        <p className="m-auto p-6 text-sm text-on-surface-variant" role="status">
          {preview.error ?? "Compilando a pré-visualização..."}
        </p>
      )}
    </div>
  );
}
