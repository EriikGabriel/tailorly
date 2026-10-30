import type { ReviewStepId } from "@@types/review-model";
import type { DocumentZoom } from "@stores/document-viewer-store";
import { useMemo, useState } from "react";
import { Document, pdfjs } from "react-pdf";
import "react-pdf/dist/Page/TextLayer.css";
import { useContainerWidth } from "../-hooks/use-container-width";
import type { EvidenceFact } from "../-types/evidence";
import { groupFactsByPage } from "../-utils/evidence-facts";
import { PdfHighlightPage } from "./pdf-highlight-page";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

/** Renderiza o PDF original via pdf.js e desenha os destaques diretamente sobre o texto detectado. */
export function PdfEvidenceViewer({
  file,
  facts,
  zoom,
  activeStep,
  focusedRecordKeys,
}: {
  file: File;
  facts: EvidenceFact[];
  zoom: DocumentZoom;
  activeStep: ReviewStepId;
  focusedRecordKeys: ReadonlySet<string>;
}) {
  const [containerRef, containerWidth] = useContainerWidth<HTMLDivElement>();
  const [numPages, setNumPages] = useState(0);
  const [loadError, setLoadError] = useState(false);

  const pageWidth =
    zoom === "fit" ? containerWidth : (containerWidth * zoom) / 100;
  const factsByPage = useMemo(() => groupFactsByPage(facts), [facts]);

  return (
    <div
      ref={containerRef}
      className="min-h-0 w-full flex-1 overflow-auto bg-surface-container-low"
    >
      {loadError ? (
        <p role="alert" className="p-6 text-body-sm text-error">
          Não foi possível carregar o PDF para destacar os pontos de extração.
        </p>
      ) : (
        <Document
          file={file}
          onLoadSuccess={({ numPages: total }) => setNumPages(total)}
          onLoadError={() => setLoadError(true)}
          loading={
            <p className="p-6 text-body-sm text-on-surface-variant">
              Carregando documento...
            </p>
          }
          className="flex flex-col items-center gap-4 p-4"
        >
          {pageWidth > 0 &&
            Array.from({ length: numPages }, (_, index) => index + 1).map(
              (pageNumber) => (
                <PdfHighlightPage
                  key={pageNumber}
                  pageNumber={pageNumber}
                  width={pageWidth}
                  facts={factsByPage.get(pageNumber) ?? []}
                  activeStep={activeStep}
                  focusedRecordKeys={focusedRecordKeys}
                />
              ),
            )}
        </Document>
      )}
    </div>
  );
}
