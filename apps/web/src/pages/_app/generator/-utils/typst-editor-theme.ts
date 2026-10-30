import { EditorView } from "@codemirror/view";

/** Tema do CodeMirror com as cores do design system; o tamanho da fonte é ajustável pelo zoom. */
export function buildTypstEditorTheme(fontSize: number) {
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
