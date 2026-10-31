import type { CvPreview } from "@hooks/use-cv-preview";
import { useEffect, useId, useMemo, useState } from "react";
import { useTypstCodeEditor } from "../-hooks/use-typst-code-editor";
import { CompileDiagnostics } from "./typst-editor/compile-diagnostics";
import { EditorStatusBar } from "./typst-editor/editor-status-bar";
import { EditorToolbar } from "./typst-editor/editor-toolbar";

interface TypstEditorProps {
  value: string;
  onChange: (value: string) => void;
  preview: CvPreview;
  /** Ctrl/Cmd+S: salvar o template e gerar de novo. */
  onSave?: () => void;
}

/**
 * Editor do template Typst: barra de ferramentas completa, autocompletar, erros do compilador no código,
 * amostras de cor, dobra de blocos, busca, zoom e tela cheia.
 */
export function TypstEditor({
  value,
  onChange,
  preview,
  onSave,
}: TypstEditorProps) {
  const labelId = useId();
  const [fullscreen, setFullscreen] = useState(false);
  const [copied, setCopied] = useState(false);
  const compiler = useMemo(
    () => ({ source: preview.compiledSource, lines: preview.diagnostics }),
    [preview.compiledSource, preview.diagnostics],
  );
  const editor = useTypstCodeEditor({
    value,
    onChange,
    compiler,
    onSave,
    onCompile: preview.compileNow,
  });

  useEffect(() => {
    if (!copied) return;
    const timer = setTimeout(() => setCopied(false), 1600);
    return () => clearTimeout(timer);
  }, [copied]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(editor.currentSource());
      setCopied(true);
    } catch {
      // Sem permissão para a área de transferência: o texto continua selecionável no editor.
    }
  }

  function download() {
    const url = URL.createObjectURL(
      new Blob([editor.currentSource()], { type: "text/plain;charset=utf-8" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "template.typ";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div
      className={
        fullscreen
          ? "fixed inset-0 z-50 flex flex-col gap-2 bg-surface p-4"
          : "flex h-full min-h-0 flex-col gap-2"
      }
    >
      <EditorToolbar
        labelId={labelId}
        fontSize={editor.fontSize}
        lineWrap={editor.lineWrap}
        fullscreen={fullscreen}
        copied={copied}
        actions={{
          undo: editor.undo,
          redo: editor.redo,
          format: editor.format,
          bold: editor.bold,
          italic: editor.italic,
          underline: editor.underline,
          inlineCode: editor.inlineCode,
          link: editor.link,
          bulletList: editor.bulletList,
          numberedList: editor.numberedList,
          insertSnippet: editor.insertSnippet,
          toggleComment: editor.toggleComment,
          indentMore: editor.indentMore,
          indentLess: editor.indentLess,
          foldAll: editor.foldAll,
          unfoldAll: editor.unfoldAll,
          search: editor.search,
          goToLine: editor.goToLine,
          zoom: editor.zoom,
          toggleLineWrap: editor.toggleLineWrap,
          copy,
          download,
          toggleFullscreen: () => setFullscreen((current) => !current),
        }}
      />

      <div
        ref={editor.containerRef}
        data-testid="typst-code"
        className="min-h-0 flex-1 overflow-hidden rounded-lg bg-card text-on-surface shadow-xs"
      />

      <EditorStatusBar cursor={editor.cursor} counts={editor.counts} />
      <CompileDiagnostics preview={preview} />
    </div>
  );
}
