import { Switch } from "@animate/base/switch";
import { Button } from "@animate/buttons/button";
import {
  ExternalLink,
  FileText,
  FileUp,
  ScanLine,
  ShieldLock,
} from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useDocumentViewerStore } from "@stores/document-viewer-store";
import { useEffect, useState } from "react";
import { DocumentZoomControls } from "./document-zoom-controls";

export function MasterDocumentPanel({
  onSelectFile,
}: {
  onSelectFile: () => void;
}) {
  const file = useBaseCvStore((state) => state.file);
  const zoom = useDocumentViewerStore((state) => state.zoom);
  const setZoom = useDocumentViewerStore((state) => state.setZoom);
  const isPdf = Boolean(file && /\.pdf$/i.test(file.name));
  const [preview, setPreview] = useState<{ file: File; url: string } | null>(
    null,
  );

  useEffect(() => {
    if (!file || !/\.pdf$/i.test(file.name)) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview({ file, url });
    return () => URL.revokeObjectURL(url);
  }, [file]);

  // Never display the previous document while a replacement URL is created.
  const url = preview?.file === file ? preview?.url : undefined;

  return (
    <section
      aria-labelledby="master-document-heading"
      className="flex min-w-0 flex-col gap-4 rounded-2xl bg-card border p-4 shadow-xs sm:p-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-3 pb-1">
        <h2
          id="master-document-heading"
          className="flex items-center gap-1 text-headline-sm text-primary-950"
        >
          <span
            aria-hidden="true"
            className="size-2.5 shrink-0 rounded-full bg-on-tertiary-container"
          />
          Documento Mestre &amp; OCR
        </h2>
        <DocumentZoomControls
          zoom={zoom}
          disabled={!url}
          onZoomChange={setZoom}
        />
      </div>

      <div className="flex items-center justify-between gap-3 rounded-xl bg-surface-container-low p-2">
        <label
          htmlFor="document-ocr-boxes"
          className="flex items-center gap-1 text-body-sm font-medium text-primary-950"
        >
          <ScanLine aria-hidden="true" className="size-4 shrink-0" />
          Bounding Boxes de IA
        </label>
        <Switch
          id="document-ocr-boxes"
          checked={false}
          disabled
          aria-describedby="document-ocr-status"
          className="h-6 w-11 px-0.5 **:data-[slot=switch-thumb]:size-5"
        />
      </div>

      <div className="flex h-130 max-h-195 min-w-0 flex-col overflow-hidden rounded-xl bg-card shadow-md sm:h-195">
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 pt-6 pb-4 text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface-variant sm:px-6">
          <span className="min-w-0 truncate" title={file?.name}>
            {file?.name ?? "Nenhum documento selecionado"}
          </span>
          <span className="shrink-0">
            {file ? "Documento original • Prévia local" : "PDF original"}
          </span>
        </div>
        {url ? (
          <iframe
            key={`${url}-${zoom}`}
            src={`${url}#toolbar=0&navpanes=0&${zoom === "fit" ? "view=Fit" : `zoom=${zoom}`}`}
            title={`Documento original: ${file?.name}`}
            className="min-h-0 w-full flex-1 border-0"
          />
        ) : (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 py-12 text-center">
            <span className="flex size-14 items-center justify-center rounded-xl bg-surface-container-low text-muted-foreground">
              <FileText aria-hidden="true" className="size-7" />
            </span>
            <div className="max-w-72 space-y-1">
              <h3 className="text-title-md">
                Seu documento original, lado a lado
              </h3>
              <p className="text-body-sm">
                {file && !isPdf
                  ? "Seu arquivo está selecionado. A visualização local está disponível apenas para PDFs."
                  : "Selecione um PDF para consultá-lo durante todas as etapas da revisão."}
              </p>
            </div>
            <Button
              variant="accent"
              onClick={onSelectFile}
              className="rounded-xl"
            >
              <FileUp aria-hidden="true" className="size-4" />
              Selecionar PDF
            </Button>
          </div>
        )}
      </div>

      {url && (
        <div className="flex items-center justify-center py-1">
          <Button
            asChild
            variant="link"
            className="h-auto justify-start whitespace-normal p-0 text-body-sm"
          >
            <a href={url} target="_blank" rel="noopener noreferrer">
              <ExternalLink aria-hidden="true" className="size-3.5" />
              Abrir PDF em outra aba
            </a>
          </Button>
        </div>
      )}

      <div className="flex items-center gap-4 rounded-xl bg-surface-container-high p-4">
        <ShieldLock
          aria-hidden="true"
          className="size-6 shrink-0 text-primary-950"
        />
        <div className="min-w-0">
          <h3 className="text-title-md text-primary-950">
            Cofre Factual Fechado
          </h3>
          <p className="text-body-sm text-on-surface-variant">
            Nenhum fato, empresa, número ou métrica além deste documento será
            inventado ou inferido pela inteligência.
          </p>
        </div>
      </div>
    </section>
  );
}
