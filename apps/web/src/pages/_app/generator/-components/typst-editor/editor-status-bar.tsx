import type { DiagnosticCounts } from "../../-hooks/use-typst-code-editor";
import type { CursorInfo } from "../../-types/typst-editor";

/** Posição do cursor, contagens do código, erros do compilador e os atalhos principais. */
export function EditorStatusBar({
  cursor,
  counts,
}: {
  cursor: CursorInfo;
  counts: DiagnosticCounts;
}) {
  const hasIssues = counts.errors + counts.warnings > 0;
  return (
    <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-0.5 rounded-lg bg-surface-container-low px-2.5 py-1 text-[11px] text-on-surface-variant">
      <span data-testid="cursor-position">
        Ln {cursor.line}, Col {cursor.column}
      </span>
      <span
        role="status"
        data-testid="editor-issues"
        className={
          counts.errors > 0
            ? "font-semibold text-error"
            : hasIssues
              ? "font-semibold"
              : ""
        }
      >
        {hasIssues
          ? `${counts.errors} ${counts.errors === 1 ? "erro" : "erros"} · ${counts.warnings} ${counts.warnings === 1 ? "aviso" : "avisos"}`
          : "Sem erros"}
      </span>
      <span className="hidden xl:inline">
        Ctrl+S salvar · Ctrl+Enter compilar · Ctrl+/ comentar
      </span>
      <span>
        {cursor.characters} caracteres · {cursor.words} palavras
      </span>
    </div>
  );
}
