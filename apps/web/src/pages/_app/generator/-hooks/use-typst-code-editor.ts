import { snippet } from "@codemirror/autocomplete";
import {
  indentLess,
  indentMore,
  indentWithTab,
  redo,
  toggleComment,
  undo,
} from "@codemirror/commands";
import { foldAll, indentUnit, unfoldAll } from "@codemirror/language";
import type { Diagnostic } from "@codemirror/lint";
import { gotoLine, openSearchPanel } from "@codemirror/search";
import {
  Compartment,
  EditorSelection,
  EditorState,
  Prec,
} from "@codemirror/state";
import { EditorView, keymap } from "@codemirror/view";
import { basicSetup } from "codemirror";
import { typst_lezer } from "codemirror-lang-typst/lezer";
import { useEffect, useRef, useState } from "react";
import {
  DEFAULT_FONT_SIZE,
  MAX_FONT_SIZE,
  MIN_FONT_SIZE,
} from "../-constants/typst-editor";
import type { CursorInfo } from "../-types/typst-editor";
import { cursorInfoFromState } from "../-utils/cursor-info";
import { formatTypstTemplate } from "../-utils/format-typst";
import {
  parseCompilerOutput,
  toEditorDiagnostics,
} from "../-utils/typst-diagnostics";
import {
  setCompilerMarks,
  typstEditorExtensions,
} from "../-utils/typst-editor-extensions";
import { buildTypstEditorTheme } from "../-utils/typst-editor-theme";
import {
  insertLink,
  type TextEdit,
  toggleLinePrefix,
  wrapSelection,
} from "../-utils/typst-edits";

const themeCompartment = new Compartment();
const wrapCompartment = new Compartment();

/** Resultado da última compilação: o código que a produziu e as linhas de erro do compilador. */
export type CompilerResult = { source: string | null; lines: string[] };

type UseTypstCodeEditorInput = {
  value: string;
  onChange: (value: string) => void;
  compiler: CompilerResult;
  /** Ctrl/Cmd+S: salvar e gerar de novo. */
  onSave?: () => void;
  /** Ctrl/Cmd+Enter: compilar a prévia agora, sem esperar a pausa na digitação. */
  onCompile?: () => void;
};

export type DiagnosticCounts = { errors: number; warnings: number };

/**
 * Ciclo de vida do CodeMirror para o template Typst: cria o editor uma vez, mantém o documento em
 * sincronia com `value`, mostra os erros do compilador no código e expõe as ações da barra de ferramentas.
 */
export function useTypstCodeEditor({
  value,
  onChange,
  compiler,
  onSave,
  onCompile,
}: UseTypstCodeEditorInput) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  const onSaveRef = useRef(onSave);
  const onCompileRef = useRef(onCompile);
  onChangeRef.current = onChange;
  onSaveRef.current = onSave;
  onCompileRef.current = onCompile;

  const [fontSize, setFontSize] = useState(DEFAULT_FONT_SIZE);
  const [lineWrap, setLineWrap] = useState(true);
  const [counts, setCounts] = useState<DiagnosticCounts>({
    errors: 0,
    warnings: 0,
  });
  const [cursor, setCursor] = useState<CursorInfo>({
    line: 1,
    column: 1,
    characters: value.length,
    words: 0,
  });

  // biome-ignore lint/correctness/useExhaustiveDependencies: o editor é recriado só na montagem
  useEffect(() => {
    if (!containerRef.current) return;

    const view = new EditorView({
      state: EditorState.create({
        doc: value,
        extensions: [
          basicSetup,
          typst_lezer(),
          typstEditorExtensions(),
          indentUnit.of("  "),
          keymap.of([indentWithTab]),
          Prec.highest(
            keymap.of([
              {
                key: "Mod-s",
                preventDefault: true,
                run: () => {
                  onSaveRef.current?.();
                  return true;
                },
              },
              {
                key: "Mod-Enter",
                preventDefault: true,
                run: () => {
                  onCompileRef.current?.();
                  return true;
                },
              },
              { key: "Shift-Alt-f", preventDefault: true, run: formatDocument },
            ]),
          ),
          themeCompartment.of(buildTypstEditorTheme(DEFAULT_FONT_SIZE)),
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
      parent: containerRef.current,
    });

    viewRef.current = view;
    setCursor(cursorInfoFromState(view.state));
    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, []);

  // Mudanças vindas de fora (ex.: "Restaurar template padrão") substituem o documento.
  useEffect(() => {
    const view = viewRef.current;
    if (view && value !== view.state.doc.toString()) {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: value },
      });
    }
  }, [value]);

  // Erros do compilador viram sublinhados no código, mas só quando pertencem ao código que está no
  // editor: se a pessoa já digitou mais, o CodeMirror vai movendo as marcas antigas até a próxima compilação.
  // biome-ignore lint/correctness/useExhaustiveDependencies: `value` reavalia se o resultado já vale para o código atual
  useEffect(() => {
    const view = viewRef.current;
    if (!view || compiler.source === null) return;
    const current = view.state.doc.toString();
    if (compiler.source !== current) return;
    const parsed = parseCompilerOutput(compiler.lines);
    const { marks } = toEditorDiagnostics(parsed, current);
    const diagnostics: Diagnostic[] = marks.map((mark) => ({
      from: mark.from,
      to: mark.to,
      severity: mark.severity,
      message: mark.message,
    }));
    view.dispatch({ effects: setCompilerMarks.of(diagnostics) });
    setCounts({
      errors: parsed.filter((item) => item.severity === "error").length,
      warnings: parsed.filter((item) => item.severity === "warning").length,
    });
  }, [compiler.source, compiler.lines, value]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: themeCompartment.reconfigure(buildTypstEditorTheme(fontSize)),
    });
  }, [fontSize]);

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: wrapCompartment.reconfigure(
        lineWrap ? EditorView.lineWrapping : [],
      ),
    });
  }, [lineWrap]);

  /** Executa uma ação sobre o editor e devolve o foco a ele. */
  function withView(action: (view: EditorView) => void) {
    const view = viewRef.current;
    if (!view) return;
    action(view);
    view.focus();
  }

  /** Aplica uma edição calculada fora do CodeMirror (ver typst-edits). */
  function applyEdit(view: EditorView, edit: TextEdit) {
    view.dispatch({
      changes: edit.changes,
      selection: EditorSelection.single(
        edit.selection.anchor,
        edit.selection.head,
      ),
      scrollIntoView: true,
      userEvent: "input",
    });
  }

  function edit(compute: (doc: string, from: number, to: number) => TextEdit) {
    withView((view) => {
      const { from, to } = view.state.selection.main;
      applyEdit(view, compute(view.state.doc.toString(), from, to));
    });
  }

  return {
    containerRef,
    cursor,
    counts,
    fontSize,
    lineWrap,
    toggleLineWrap: () => setLineWrap((current) => !current),
    zoom(delta: number) {
      setFontSize((current) =>
        Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, current + delta)),
      );
      viewRef.current?.focus();
    },
    search: () => withView(openSearchPanel),
    undo: () => withView(undo),
    redo: () => withView(redo),
    format: () => withView((view) => formatDocument(view)),
    bold: () =>
      edit((doc, from, to) =>
        wrapSelection(doc, from, to, "*", "*", "negrito"),
      ),
    italic: () =>
      edit((doc, from, to) =>
        wrapSelection(doc, from, to, "_", "_", "itálico"),
      ),
    underline: () =>
      edit((doc, from, to) =>
        wrapSelection(doc, from, to, "#underline[", "]", "sublinhado"),
      ),
    inlineCode: () =>
      edit((doc, from, to) => wrapSelection(doc, from, to, "`", "`", "código")),
    link: () => edit((doc, from, to) => insertLink(doc, from, to)),
    bulletList: () =>
      edit((doc, from, to) => toggleLinePrefix(doc, from, to, "- ")),
    numberedList: () =>
      edit((doc, from, to) => toggleLinePrefix(doc, from, to, "+ ")),
    toggleComment: () => withView((view) => toggleComment(view)),
    indentMore: () => withView((view) => indentMore(view)),
    indentLess: () => withView((view) => indentLess(view)),
    foldAll: () => withView((view) => foldAll(view)),
    unfoldAll: () => withView((view) => unfoldAll(view)),
    goToLine: () => withView((view) => gotoLine(view)),
    /** Insere um snippet do CodeMirror (campos `${1:padrão}`) na seleção atual. */
    insertSnippet: (template: string) =>
      withView((view) => {
        const { from, to } = view.state.selection.main;
        snippet(template)(view, null, from, to);
        // Chamado pelo menu "Inserir": ele devolve o foco ao fechar, então o editor o retoma no quadro seguinte.
        requestAnimationFrame(() => view.focus());
      }),
    /** Código atual do template, para copiar ou baixar. */
    currentSource: () => viewRef.current?.state.doc.toString() ?? value,
  };
}

function formatDocument(view: EditorView): boolean {
  const current = view.state.doc.toString();
  const formatted = formatTypstTemplate(current);
  if (formatted !== current) {
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: formatted },
    });
  }
  return true;
}
