/** biome-ignore-all lint/a11y/noLabelWithoutControl: o rótulo nomeia a área do editor, que não é um input */

import {
  AlignLeft,
  Bold,
  Check,
  ChevronsDownUp,
  ChevronsUpDown,
  Code,
  Copy,
  FileDown,
  Hash,
  Indent,
  Italic,
  Link,
  List,
  ListOrdered,
  Maximize2,
  MessageSquareCode,
  Minimize2,
  Outdent,
  Redo2,
  Search,
  Underline,
  Undo2,
  WrapText,
  ZoomIn,
  ZoomOut,
} from "@react-zero-ui/icon-sprite";
import { MAX_FONT_SIZE, MIN_FONT_SIZE } from "../../-constants/typst-editor";
import { InsertMenu } from "./insert-menu";
import { ToolbarButton } from "./toolbar-button";

export type EditorToolbarActions = {
  undo: () => void;
  redo: () => void;
  format: () => void;
  bold: () => void;
  italic: () => void;
  underline: () => void;
  inlineCode: () => void;
  link: () => void;
  bulletList: () => void;
  numberedList: () => void;
  insertSnippet: (template: string) => void;
  toggleComment: () => void;
  indentMore: () => void;
  indentLess: () => void;
  foldAll: () => void;
  unfoldAll: () => void;
  search: () => void;
  goToLine: () => void;
  zoom: (delta: number) => void;
  toggleLineWrap: () => void;
  copy: () => void;
  download: () => void;
  toggleFullscreen: () => void;
};

type EditorToolbarProps = {
  labelId: string;
  fontSize: number;
  lineWrap: boolean;
  fullscreen: boolean;
  copied: boolean;
  actions: EditorToolbarActions;
};

const ICON = "size-3.75";
const DIVIDER = (
  <span className="mx-1 h-4.5 w-px bg-outline-variant" aria-hidden="true" />
);

/**
 * Barra de ferramentas do editor Typst, em grupos: histórico e formatação do código, estilo de texto,
 * inserção de trechos, comentários e indentação, dobra de blocos, busca, zoom e arquivo.
 */
export function EditorToolbar({
  labelId,
  fontSize,
  lineWrap,
  fullscreen,
  copied,
  actions,
}: EditorToolbarProps) {
  return (
    <div className="rounded-lg bg-card px-2 py-1 shadow-xs">
      <span id={labelId} className="sr-only">
        Template Typst
      </span>
      <div
        role="toolbar"
        aria-labelledby={labelId}
        className="flex flex-nowrap items-center gap-px overflow-x-auto [scrollbar-width:thin]"
      >
        <ToolbarButton label="Desfazer (Ctrl+Z)" onClick={actions.undo}>
          <Undo2 className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Refazer (Ctrl+Shift+Z)" onClick={actions.redo}>
          <Redo2 className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label="Formatar código (Alt+Shift+F)"
          onClick={actions.format}
        >
          <AlignLeft className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton label="Negrito" onClick={actions.bold}>
          <Bold className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Itálico" onClick={actions.italic}>
          <Italic className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Sublinhado" onClick={actions.underline}>
          <Underline className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Código em linha" onClick={actions.inlineCode}>
          <Code className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Link" onClick={actions.link}>
          <Link className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label="Lista com marcadores"
          onClick={actions.bulletList}
        >
          <List className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Lista numerada" onClick={actions.numberedList}>
          <ListOrdered className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <InsertMenu onInsert={actions.insertSnippet} />
        <ToolbarButton
          label="Comentar ou descomentar (Ctrl+/)"
          onClick={actions.toggleComment}
        >
          <MessageSquareCode className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Diminuir recuo" onClick={actions.indentLess}>
          <Outdent className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton label="Aumentar recuo" onClick={actions.indentMore}>
          <Indent className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label="Recolher todos os blocos"
          onClick={actions.foldAll}
        >
          <ChevronsDownUp className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label="Expandir todos os blocos"
          onClick={actions.unfoldAll}
        >
          <ChevronsUpDown className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton
          label="Buscar e substituir (Ctrl+F)"
          onClick={actions.search}
        >
          <Search className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label="Ir para a linha (Ctrl+Alt+G)"
          onClick={actions.goToLine}
        >
          <Hash className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton
          label="Diminuir fonte"
          onClick={() => actions.zoom(-1)}
          disabled={fontSize <= MIN_FONT_SIZE}
        >
          <ZoomOut className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <span className="w-7 text-center text-[11px] tabular-nums text-on-surface-variant">
          {fontSize}px
        </span>
        <ToolbarButton
          label="Aumentar fonte"
          onClick={() => actions.zoom(1)}
          disabled={fontSize >= MAX_FONT_SIZE}
        >
          <ZoomIn className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label={
            lineWrap ? "Desativar quebra de linha" : "Ativar quebra de linha"
          }
          onClick={actions.toggleLineWrap}
          active={lineWrap}
        >
          <WrapText className={ICON} aria-hidden="true" />
        </ToolbarButton>
        {DIVIDER}
        <ToolbarButton
          label={copied ? "Código copiado" : "Copiar código"}
          onClick={actions.copy}
        >
          {copied ? (
            <Check className={ICON} aria-hidden="true" />
          ) : (
            <Copy className={ICON} aria-hidden="true" />
          )}
        </ToolbarButton>
        <ToolbarButton label="Baixar código (.typ)" onClick={actions.download}>
          <FileDown className={ICON} aria-hidden="true" />
        </ToolbarButton>
        <ToolbarButton
          label={fullscreen ? "Sair da tela cheia" : "Tela cheia"}
          onClick={actions.toggleFullscreen}
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
