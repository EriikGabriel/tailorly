import type { CvPreviewState } from "@hooks/use-cv-preview";
import { useId, useState } from "react";
import { useTypstCodeEditor } from "../-hooks/use-typst-code-editor";
import { CompileDiagnostics } from "./typst-editor/compile-diagnostics";
import { EditorStatusBar } from "./typst-editor/editor-status-bar";
import { EditorToolbar } from "./typst-editor/editor-toolbar";

interface TypstEditorProps {
  value: string;
  onChange: (value: string) => void;
  preview: CvPreviewState;
}

/** Editor do template Typst, inspirado no TeXlyre: barra de ferramentas, busca, zoom e tela cheia. */
export function TypstEditor({ value, onChange, preview }: TypstEditorProps) {
  const labelId = useId();
  const [fullscreen, setFullscreen] = useState(false);
  const editor = useTypstCodeEditor({ value, onChange });

  return (
    <div
      className={
        fullscreen
          ? "fixed inset-0 z-50 flex flex-col gap-2 bg-surface p-4"
          : "flex h-[min(1120px,141.4vw)] max-h-[min(1120px,141.4vw)] flex-col gap-2"
      }
    >
      <EditorToolbar
        labelId={labelId}
        fontSize={editor.fontSize}
        lineWrap={editor.lineWrap}
        fullscreen={fullscreen}
        onSearch={editor.search}
        onUndo={editor.undo}
        onRedo={editor.redo}
        onFormat={editor.format}
        onZoom={editor.zoom}
        onToggleLineWrap={editor.toggleLineWrap}
        onToggleFullscreen={() => setFullscreen((current) => !current)}
      />

      <div
        ref={editor.containerRef}
        className="min-h-0 flex-1 overflow-hidden rounded-lg bg-card text-on-surface shadow-xs"
      />

      <EditorStatusBar cursor={editor.cursor} />
      <CompileDiagnostics preview={preview} />
    </div>
  );
}
