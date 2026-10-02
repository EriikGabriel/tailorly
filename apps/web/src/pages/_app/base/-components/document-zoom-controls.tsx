import { Button } from "@animate/buttons/button";
import { Minus, Plus } from "@react-zero-ui/icon-sprite";

import type { DocumentZoom } from "@stores/document-viewer-store";

export function DocumentZoomControls({
  zoom,
  disabled,
  onZoomChange,
}: {
  zoom: DocumentZoom;
  disabled: boolean;
  onZoomChange: (zoom: DocumentZoom) => void;
}) {
  return (
    <fieldset
      aria-label="Zoom do documento"
      className="flex shrink-0 items-center gap-1 rounded-lg bg-surface-container px-2 py-1"
    >
      <Button
        variant="ghost"
        size="icon-sm"
        disabled={disabled || (typeof zoom === "number" && zoom <= 50)}
        aria-label="Diminuir zoom"
        onClick={() => onZoomChange(zoom === "fit" ? 50 : zoom - 25)}
        className="size-6 rounded-sm text-primary-950"
      >
        <Minus aria-hidden="true" className="size-3" />
      </Button>
      <Button
        variant="ghost"
        disabled={disabled}
        aria-label="Ajustar página inteira à área de visualização"
        aria-pressed={zoom === "fit"}
        title="Ajustar página inteira"
        onClick={() => onZoomChange("fit")}
        className="h-6 min-w-11 rounded-sm px-1 text-center text-xs font-medium leading-4 tracking-[0.3px] text-primary-950"
      >
        {zoom === "fit" ? "Fit" : `${zoom}%`}
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        disabled={disabled || (typeof zoom === "number" && zoom >= 200)}
        aria-label="Aumentar zoom"
        onClick={() => onZoomChange(zoom === "fit" ? 100 : zoom + 25)}
        className="size-6 rounded-sm text-primary-950"
      >
        <Plus aria-hidden="true" className="size-3" />
      </Button>
    </fieldset>
  );
}
