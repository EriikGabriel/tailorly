import { Button } from "@animate/buttons/button";
import type { ExtractedPageResponse } from "@lib/api/generated/model/extractedPageResponse";
import { useExtractedPageText } from "@queries/base-review";
import { FileText, ScanLine } from "@react-zero-ui/icon-sprite";
import { useEffect, useState } from "react";

type ExtractedTextPanelProps = {
  pages: ExtractedPageResponse[];
  loading: boolean;
};

/**
 * Exibe o resultado bruto do PDF/OCR para conferência. Ele não preenche campos
 * nem cria fatos: essa decisão pertence à etapa de interpretação e revisão.
 */
export function ExtractedTextPanel({
  pages,
  loading,
}: ExtractedTextPanelProps) {
  const [selectedPageId, setSelectedPageId] = useState<string>();
  const selectedPage =
    pages.find((page) => page.id === selectedPageId) ?? pages[0];
  const text = useExtractedPageText(selectedPage?.id);

  useEffect(() => {
    if (!selectedPageId && pages[0]) {
      setSelectedPageId(pages[0].id);
    }
  }, [pages, selectedPageId]);

  return (
    <details className="rounded-xl bg-surface-container-low p-4">
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-title-md text-primary-950">
        <span className="flex items-center gap-2">
          <ScanLine aria-hidden="true" className="size-4" />
          Texto extraído
        </span>
        <span className="text-body-sm font-normal text-on-surface-variant">
          {loading
            ? "Carregando..."
            : pages.length
              ? `${pages.length} página${pages.length === 1 ? "" : "s"}`
              : "Aguardando extração"}
        </span>
      </summary>

      <div className="mt-4 space-y-3 border-t pt-4">
        {loading ? (
          <p className="text-body-sm text-on-surface-variant">
            Buscando as páginas processadas.
          </p>
        ) : pages.length === 0 ? (
          <p className="text-body-sm text-on-surface-variant">
            O texto aparecerá aqui quando a extração for concluída.
          </p>
        ) : (
          <>
            <fieldset className="flex flex-wrap gap-2">
              <legend className="sr-only">Páginas extraídas</legend>
              {pages.map((page) => (
                <Button
                  key={page.id}
                  type="button"
                  variant={page.id === selectedPage?.id ? "default" : "accent"}
                  onClick={() => setSelectedPageId(page.id)}
                  className="h-8 rounded-lg px-3 text-xs"
                >
                  Página {page.pageNumber} · {page.extractionMethod}
                </Button>
              ))}
            </fieldset>
            {text.isLoading ? (
              <p className="text-body-sm text-on-surface-variant">
                Carregando texto da página.
              </p>
            ) : text.isError ? (
              <p role="alert" className="text-body-sm text-error">
                Não foi possível carregar o texto extraído.
              </p>
            ) : (
              <pre className="max-h-72 overflow-auto whitespace-pre-wrap rounded-lg bg-card p-3 text-body-sm text-on-surface-variant">
                <FileText aria-hidden="true" className="mr-2 inline size-4" />
                {text.data?.text || "Esta página não contém texto utilizável."}
              </pre>
            )}
          </>
        )}
      </div>
    </details>
  );
}
