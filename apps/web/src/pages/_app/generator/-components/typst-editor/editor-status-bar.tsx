import type { CursorInfo } from "../../-types/typst-editor";

/** Posição do cursor e contagem de caracteres e palavras do template. */
export function EditorStatusBar({ cursor }: { cursor: CursorInfo }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-2.5 py-1 text-[11px] text-on-surface-variant">
      <span>
        Ln {cursor.line}, Col {cursor.column}
      </span>
      <span>
        {cursor.characters} caracteres · {cursor.words} palavras
      </span>
    </div>
  );
}
