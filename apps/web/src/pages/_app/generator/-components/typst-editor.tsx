/** biome-ignore-all lint/a11y/noLabelWithoutControl: off > */

import { Tooltip, TooltipPanel, TooltipTrigger } from "@animate/base/tooltip";
import { autocompletion } from "@codemirror/autocomplete";
import { indentWithTab, redo, undo } from "@codemirror/commands";
import { indentUnit } from "@codemirror/language";
import { openSearchPanel } from "@codemirror/search";
import { Compartment, EditorState } from "@codemirror/state";
import { EditorView, keymap } from "@codemirror/view";
import type { CvPreviewState } from "@hooks/use-cv-preview";
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
import { basicSetup } from "codemirror";
import { typst_lezer } from "codemirror-lang-typst/lezer";
import { useEffect, useId, useRef, useState } from "react";
import { formatTypstTemplate } from "./format-typst";

interface TypstEditorProps {
  value: string;
  onChange: (value: string) => void;
  preview: CvPreviewState;
}

const MIN_FONT_SIZE = 11;
const MAX_FONT_SIZE = 20;
const DEFAULT_FONT_SIZE = 13;

const themeCompartment = new Compartment();
const wrapCompartment = new Compartment();

function buildTheme(fontSize: number) {
  return EditorView.theme({
    "&": {
      height: "100%",
      backgroundColor: "var(--color-card)",
      color: "var(--color-on-surface)",
    },
    ".cm-scroller": {
      overflow: "auto",
      fontFamily:
        "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
      fontSize: `${fontSize}px`,
      lineHeight: "1.6",
    },
    ".cm-content": {
      minHeight: "100%",
      padding: "0.75rem",
      overflowWrap: "anywhere",
    },
    ".cm-gutters": {
      backgroundColor: "var(--color-card)",
      border: "none",
      color: "var(--color-on-surface-variant)",
    },
    ".cm-activeLine": {
      backgroundColor: "var(--color-surface-container)",
    },
    ".cm-activeLineGutter": {
      backgroundColor: "var(--color-surface-container)",
    },
    "&.cm-focused": {
      outline: "2px solid var(--color-ring)",
      outlineOffset: "-2px",
    },
    ".cm-searchMatch": {
      backgroundColor: "var(--color-secondary-container)",
    },
    ".cm-searchMatch.cm-searchMatch-selected": {
      backgroundColor: "var(--color-primary-900)",
      color: "var(--color-primary-foreground)",
    },
    ".cm-panels": {
      backgroundColor: "var(--color-surface-container)",
      color: "var(--color-on-surface)",
    },
    ".cm-panels.cm-panels-top": {
      borderBottom: "1px solid var(--color-outline-variant)",
    },
  });
}

interface CursorInfo {
  line: number;
  column: number;
  characters: number;
  words: number;
}

function cursorInfoFromState(state: EditorState): CursorInfo {
  const pos = state.selection.main.head;
  const line = state.doc.lineAt(pos);
  const text = state.doc.toString();
  const words = text.trim().length === 0 ? 0 : text.trim().split(/\s+/).length;
  return {
    line: line.number,
    column: pos - line.from + 1,
    characters: text.length,
    words,
  };
}

/** Editor do template Typst, inspirado no TeXlyre: barra de ferramentas, busca, zoom e tela cheia. */
export function TypstEditor({ value, onChange, preview }: TypstEditorProps) {
  const labelId = useId();
  const editorRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  const [fontSize, setFontSize] = useState(DEFAULT_FONT_SIZE);
  const [lineWrap, setLineWrap] = useState(true);
  const [fullscreen, setFullscreen] = useState(false);
  const [cursor, setCursor] = useState<CursorInfo>({
    line: 1,
    column: 1,
    characters: value.length,
    words: 0,
  });

  // biome-ignore lint/correctness/useExhaustiveDependencies: o editor é recriado só na montagem
  useEffect(() => {
    if (!editorRef.current) {
      return;
    }

    const view = new EditorView({
      state: EditorState.create({
        doc: value,
        extensions: [
          basicSetup,
          typst_lezer(),
          autocompletion(),
          indentUnit.of("  "),
          keymap.of([indentWithTab]),
          themeCompartment.of(buildTheme(DEFAULT_FONT_SIZE)),
          wrapCompartment.of(EditorView.lineWrapping),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              onChangeRef.current(update.state.doc.toString());
            }
            if (update.docChanged || update.selectionSet) {
              setCursor(cursorInfoFromState(update.state));
            }
          }),
        ],
      }),
      parent: editorRef.current,
    });

    viewRef.current = view;
    setCursor(cursorInfoFromState(view.state));
    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, []);

  useEffect(() => {
    const view = viewRef.current;
    if (view && value !== view.state.doc.toString()) {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: value },
      });
    }
  }, [value]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: themeCompartment.reconfigure(buildTheme(fontSize)),
    });
  }, [fontSize]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: wrapCompartment.reconfigure(
        lineWrap ? EditorView.lineWrapping : [],
      ),
    });
  }, [lineWrap]);

  function focusView() {
    viewRef.current?.focus();
  }

  function zoom(delta: number) {
    setFontSize((current) =>
      Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, current + delta)),
    );
    focusView();
  }

  function triggerSearch() {
    const view = viewRef.current;
    if (!view) return;
    openSearchPanel(view);
    view.focus();
  }

  function triggerUndo() {
    const view = viewRef.current;
    if (!view) return;
    undo(view);
    view.focus();
  }

  function triggerRedo() {
    const view = viewRef.current;
    if (!view) return;
    redo(view);
    view.focus();
  }

  function triggerFormat() {
    const view = viewRef.current;
    if (!view) return;
    const formatted = formatTypstTemplate(view.state.doc.toString());
    if (formatted === view.state.doc.toString()) return;
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: formatted },
    });
    view.focus();
  }

  return (
    <div
      className={
        fullscreen
          ? "fixed inset-0 z-50 flex flex-col gap-2 bg-surface p-4"
          : "flex h-[min(1120px,141.4vw)] max-h-[min(1120px,141.4vw)] flex-col gap-2"
      }
    >
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-card px-2.5 py-1.5 shadow-xs">
        <label id={labelId} className="text-xs font-semibold text-on-surface">
          Template Typst
        </label>
        <div className="flex items-center gap-0.5">
          <ToolbarButton label="Buscar (Ctrl+F)" onClick={triggerSearch}>
            <Search className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton label="Desfazer" onClick={triggerUndo}>
            <Undo2 className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton label="Refazer" onClick={triggerRedo}>
            <Redo2 className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton label="Formatar código" onClick={triggerFormat}>
            <AlignLeft className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <span className="mx-1 h-4.5 w-px bg-outline-variant" />
          <ToolbarButton
            label="Diminuir fonte"
            onClick={() => zoom(-1)}
            disabled={fontSize <= MIN_FONT_SIZE}
          >
            <ZoomOut className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <span className="w-7 text-center text-[11px] tabular-nums text-on-surface-variant">
            {fontSize}px
          </span>
          <ToolbarButton
            label="Aumentar fonte"
            onClick={() => zoom(1)}
            disabled={fontSize >= MAX_FONT_SIZE}
          >
            <ZoomIn className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <span className="mx-1 h-4.5 w-px bg-outline-variant" />
          <ToolbarButton
            label={
              lineWrap ? "Desativar quebra de linha" : "Ativar quebra de linha"
            }
            onClick={() => setLineWrap((current) => !current)}
            active={lineWrap}
          >
            <WrapText className="size-3.75" aria-hidden="true" />
          </ToolbarButton>
          <ToolbarButton
            label={fullscreen ? "Sair da tela cheia" : "Tela cheia"}
            onClick={() => setFullscreen((current) => !current)}
          >
            {fullscreen ? (
              <Minimize2 className="size-3.75" aria-hidden="true" />
            ) : (
              <Maximize2 className="size-3.75" aria-hidden="true" />
            )}
          </ToolbarButton>
        </div>
      </div>

      <div
        ref={editorRef}
        className="min-h-0 flex-1 overflow-hidden rounded-lg bg-card text-on-surface shadow-xs"
      />

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-2.5 py-1 text-[11px] text-on-surface-variant">
        <span>
          Ln {cursor.line}, Col {cursor.column}
        </span>
        <span>
          {cursor.characters} caracteres · {cursor.words} palavras
        </span>
      </div>

      {preview.error && (
        <div
          role="alert"
          className="max-h-40 overflow-auto rounded-lg bg-error-container p-2.5 text-xs text-on-error-container"
        >
          <p className="font-semibold">{preview.error}</p>
          {preview.diagnostics.length > 0 && (
            <pre className="mt-1 whitespace-pre-wrap font-mono">
              {preview.diagnostics.join("\n")}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}

function ToolbarButton({
  label,
  onClick,
  disabled,
  active,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  active?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Tooltip>
      <TooltipTrigger
        type="button"
        onClick={onClick}
        disabled={disabled}
        aria-label={label}
        aria-pressed={active}
        className={`flex size-7 items-center justify-center rounded-md text-on-surface-variant outline-none transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-40 disabled:hover:bg-transparent ${
          active ? "bg-secondary-container text-on-secondary-container" : ""
        }`}
      >
        {children}
      </TooltipTrigger>
      <TooltipPanel>{label}</TooltipPanel>
    </Tooltip>
  );
}
