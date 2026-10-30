import type { ReviewStepId } from "@@types/review-model";
import { useCallback, useRef, useState } from "react";
import type { EvidenceFact, PageHighlights } from "../-types/evidence";
import { computePageHighlights } from "../-utils/page-highlights";

const EMPTY_HIGHLIGHTS: PageHighlights = {
  marks: [],
  groups: [],
  matchedFactIds: new Set(),
};

type UsePageHighlightsInput = {
  facts: EvidenceFact[];
  activeStep: ReviewStepId;
  focusedRecordKeys: ReadonlySet<string>;
};

/**
 * Calcula os destaques de uma página do PDF assim que o pdf.js termina de renderizar a camada
 * de texto. Devolve `wrapperRef` (a página), `computeHighlights` (para o `onRenderTextLayerSuccess`)
 * e o resultado.
 */
export function usePageHighlights({
  facts,
  activeStep,
  focusedRecordKeys,
}: UsePageHighlightsInput) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [highlights, setHighlights] =
    useState<PageHighlights>(EMPTY_HIGHLIGHTS);
  const [pageHasText, setPageHasText] = useState(true);

  // Precisa de identidade estável: react-pdf reconstrói a camada de texto
  // inteira sempre que este callback muda de referência (ela entra nas
  // dependências do useLayoutEffect que a renderiza). Sem useCallback, cada
  // render criava uma função nova, o que recriava a camada de texto, o que
  // disparava este callback de novo — um loop que destruía os spans sob o
  // cursor continuamente e derrubava qualquer seleção em andamento.
  const computeHighlights = useCallback(() => {
    const wrapper = wrapperRef.current;
    const textLayer = wrapper?.querySelector(".react-pdf__Page__textContent");
    if (!wrapper || !textLayer) {
      setHighlights(EMPTY_HIGHLIGHTS);
      return;
    }

    // Páginas escaneadas (OCR) não têm camada de texto selecionável no pdf.js:
    // não há span algum para ancorar um destaque, mesmo que o fato exista.
    const hasText = textLayer.querySelector("span") !== null;
    setPageHasText(hasText);
    if (!hasText || facts.length === 0) {
      setHighlights(EMPTY_HIGHLIGHTS);
      return;
    }

    setHighlights(
      computePageHighlights({
        facts,
        textLayer,
        wrapperRect: wrapper.getBoundingClientRect(),
        activeStep,
        focusedRecordKeys,
      }),
    );
  }, [facts, activeStep, focusedRecordKeys]);

  return { wrapperRef, computeHighlights, highlights, pageHasText };
}
