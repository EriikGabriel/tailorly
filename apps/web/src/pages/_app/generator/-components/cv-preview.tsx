import type { CvPreviewState } from "@hooks/use-cv-preview";

/** PDF compilado pelo servidor; o último PDF válido continua na tela se o código atual não compilar. */
export function CvPreview({ preview }: { preview: CvPreviewState }) {
  return (
    <div className="relative flex h-full min-h-[min(1120px,141.4vw)] w-full flex-col overflow-hidden rounded-lg bg-card shadow-md">
      {preview.url ? (
        <iframe
          title="Pré-visualização do currículo em PDF"
          src={preview.url}
          className="h-full w-full flex-1 border-0"
        />
      ) : (
        <p className="m-auto p-6 text-sm text-on-surface-variant" role="status">
          {preview.error ?? "Compilando a pré-visualização..."}
        </p>
      )}
      {preview.isCompiling && (
        <span
          role="status"
          className="absolute right-3 top-3 rounded-full bg-primary-900 px-2.5 py-1 text-[11px] font-semibold text-primary-foreground"
        >
          Compilando...
        </span>
      )}
    </div>
  );
}
