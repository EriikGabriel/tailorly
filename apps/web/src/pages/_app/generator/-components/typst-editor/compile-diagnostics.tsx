import type { CvPreviewState } from "@hooks/use-cv-preview";

/** Erro de compilação do Typst e seus diagnósticos; não renderiza nada quando a prévia compila. */
export function CompileDiagnostics({ preview }: { preview: CvPreviewState }) {
  if (!preview.error) return null;
  return (
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
  );
}
