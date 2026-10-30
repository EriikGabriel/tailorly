import type { ReviewStepId } from "@@types/review-model";
import { Page } from "react-pdf";
import { usePageHighlights } from "../-hooks/use-page-highlights";
import type { EvidenceFact } from "../-types/evidence";
import {
  HighlightGroupBox,
  HighlightMarkBox,
} from "./evidence-highlight-overlay";

/** Uma página do PDF com os destaques das evidências desenhados sobre o texto detectado. */
export function PdfHighlightPage({
  pageNumber,
  width,
  facts,
  activeStep,
  focusedRecordKeys,
}: {
  pageNumber: number;
  width: number;
  facts: EvidenceFact[];
  activeStep: ReviewStepId;
  focusedRecordKeys: ReadonlySet<string>;
}) {
  const { wrapperRef, computeHighlights, highlights, pageHasText } =
    usePageHighlights({ facts, activeStep, focusedRecordKeys });

  return (
    <div ref={wrapperRef} className="relative shadow-md">
      <Page
        pageNumber={pageNumber}
        width={width}
        renderAnnotationLayer={false}
        onRenderTextLayerSuccess={computeHighlights}
      />

      {facts.length > 0 && !pageHasText && (
        <span className="pointer-events-none absolute inset-x-2 top-2 select-none rounded-md bg-error/90 px-2 py-1 text-center text-[11px] font-semibold text-on-error">
          Página processada por OCR (documento escaneado): sem camada de texto
          para destacar diretamente aqui.
        </span>
      )}
      {facts.length > 0 && pageHasText && (
        <span className="pointer-events-none absolute left-2 top-2 z-20 select-none rounded-md bg-primary-950/80 px-2 py-0.5 text-[11px] font-semibold text-white">
          {highlights.matchedFactIds.size}/{facts.length} marcados
        </span>
      )}

      {highlights.groups.map((group) => (
        <HighlightGroupBox key={group.id} group={group} />
      ))}
      {highlights.marks.map((mark) => (
        <HighlightMarkBox key={mark.id} mark={mark} />
      ))}
    </div>
  );
}
