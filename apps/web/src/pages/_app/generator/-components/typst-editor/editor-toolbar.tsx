/** biome-ignore-all lint/a11y/noLabelWithoutControl: o rótulo nomeia a área do editor, que não é um input */

import {
  AlignLeft,
  Maximize2,
  Minimize2,
  Redo2,
  Search,
  Undo2,
  WrapText,
  ZoomIn,
  ZoomOut,
} from "@react-zero-ui/icon-sprite";
import { MAX_FONT_SIZE, MIN_FONT_SIZE } from "../../-constants/typst-editor";
import { ToolbarButton } from "./toolbar-button";

type EditorToolbarProps = {
  labelId: string;
  fontSize: number;
  lineWrap: boolean;
  fullscreen: boolean;
  onSearch: () => void;
  onUndo: () => void;
  onRedo: () => void;
  onFormat: () => void;
  onZoom: (delta: number) => void;
  onToggleLineWrap: () => void;
  onToggleFullscreen: () => void;
};

const ICON = "size-3.75";
const DIVIDER = <span className="mx-1 h-4.5 w-px bg-outline-variant" />;

/** Barra de ferramentas do editor: busca, desfazer/refazer, formatar, zoom, quebra de linha e tela cheia. */
export function EditorToolbar({
  labelId,
  fontSize,
  lineWrap,
  fullscreen,
  onSearch,
  onUndo,
  onRedo,
  onFormat,
  onZoom,
  onToggleLineWrap,
  onToggleFullscreen,
}: EditorToolbarProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-card px-2.5 py-1.5 shadow-xs">
      <label id={labelId} className="text-xs font-semibold text-on-surface">
        Template Typst
      </label>
      <div className="flex items-center gap-0.5">
        <ToolbarButton label="Buscar (Ctrl+F)" onClick={onSearch}>
          <Search className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Desfazer" onClick={onUndo}>
          <Undo2 className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Refazer" onClick={onRedo}>
          <Redo2 className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Formatar código" onClick={onFormat}>
          <AlignLeft className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton
          label="Diminuir fonte"
          onClick={() => onZoom(-1)}
          disabled={fontSize <= MIN_FONT_SIZE}
        >
          <ZoomOut className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <span className="w-7 text-center text-[11px] tabular-nums text-on-surface-variant">
          {fontSize}px
        </span>
        <ToolbarButton
          label="Aumentar fonte"
          onClick={() => onZoom(1)}
          disabled={fontSize >= MAX_FONT_SIZE}
        >
          <ZoomIn className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton
          label={
            lineWrap ? "Desativar quebra de linha" : "Ativar quebra de linha"
          }
          onClick={onToggleLineWrap}
          active={lineWrap}
        >
          <WrapText className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label={fullscreen ? "Sair da tela cheia" : "Tela cheia"}
          onClick={onToggleFullscreen}
        >
          {fullscreen ? (
            <Minimize2 className={ICON} aria-hidden="true" />
          ) : (
            <Maximize2 className={ICON} aria-hidden="true" />
          )}
        </ToolbarButton>
      </div>
    </div>
  );
}
