import type { CvPreviewState } from "@hooks/use-cv-preview";
import { useId } from "react";

interface TypstEditorProps {
  value: string;
  onChange: (value: string) => void;
  preview: CvPreviewState;
}

/** Editor do template Typst; as mudanças aparecem na prévia em segundos, sem salvar. */
export function TypstEditor({ value, onChange, preview }: TypstEditorProps) {
  const id = useId();
  return (
    <div className="flex h-full min-h-[min(1120px,141.4vw)] flex-col gap-2">
      <label htmlFor={id} className="text-xs font-semibold text-on-surface">
        Template Typst
      </label>
      <textarea
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        spellCheck={false}
        wrap="off"
        className="min-h-0 flex-1 resize-none rounded-lg bg-card p-3 font-mono text-xs leading-5 text-on-surface shadow-xs outline-none focus-visible:outline-2 focus-visible:outline-ring"
      />
      {preview.error && (
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
      )}
    </div>
  );
}
