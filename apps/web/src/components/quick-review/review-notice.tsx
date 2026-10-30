import type { ReactNode } from "react";

/** Aviso informativo dentro do diálogo (ex.: pendências fora da vaga, revisão completa necessária). */
export function ReviewNotice({ children }: { children: ReactNode }) {
  return (
    <p className="rounded-lg border border-outline-variant/40 bg-surface-container-low p-3 text-sm text-on-surface-variant">
      {children}
    </p>
  );
}
