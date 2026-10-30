import { autocompletion } from "@codemirror/autocomplete";
import { indentWithTab, redo, undo } from "@codemirror/commands";
import { indentUnit } from "@codemirror/language";
import { openSearchPanel } from "@codemirror/search";
import { Compartment, EditorState } from "@codemirror/state";
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
import { buildTypstEditorTheme } from "../-utils/typst-editor-theme";

const themeCompartment = new Compartment();
const wrapCompartment = new Compartment();

type UseTypstCodeEditorInput = {
  value: string;
  onChange: (value: string) => void;
};

/**
 * Ciclo de vida do CodeMirror para o template Typst: cria o editor uma vez, mantém o documento em
 * sincronia com `value` e expõe fonte, quebra de linha e as ações da barra de ferramentas.
 */
export function useTypstCodeEditor({
  value,
  onChange,
}: UseTypstCodeEditorInput) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  const [fontSize, setFontSize] = useState(DEFAULT_FONT_SIZE);
  const [lineWrap, setLineWrap] = useState(true);
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
          autocompletion(),
          indentUnit.of("  "),
          keymap.of([indentWithTab]),
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

  return {
    containerRef,
    cursor,
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
    format: () =>
      withView((view) => {
        const current = view.state.doc.toString();
        const formatted = formatTypstTemplate(current);
        if (formatted === current) return;
        view.dispatch({
          changes: { from: 0, to: view.state.doc.length, insert: formatted },
        });
      }),
  };
}
