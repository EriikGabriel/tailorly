import type { ReviewStepId } from "@@types/review-model";
import type { DocumentZoom } from "@stores/document-viewer-store";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/Page/TextLayer.css";
import { findEvidenceSpans } from "@/utils/pdf-text-match";
import type { EvidenceFact } from "./evidence-facts";
import {
  categoryLabel,
  EVIDENCE_TONE,
  isCategoryActive,
} from "./evidence-style";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

type Rect = { top: number; left: number; width: number; height: number };
type Mark = { id: string; category: string; focused: boolean; rect: Rect };
type Group = {
  id: string;
  category: string;
  focused: boolean;
  rect: Rect;
};

// Distância vertical (px) abaixo da qual duas marcações do mesmo registro são
// tratadas como parte do mesmo bloco (ex.: linhas de um mesmo cargo), em vez
// de dois blocos separados.
const CLUSTER_GAP = 36;
const GROUP_PADDING = 10;

function isFactFocused(
  fact: EvidenceFact,
  activeStep: ReviewStepId,
  focusedRecordKeys: ReadonlySet<string>,
): boolean {
  // Categorias sem registro específico (ex.: identidade) acompanham a etapa
  // inteira; as repetíveis só entram em foco quando o cartão daquela entrada
  // está aberto no formulário à direita.
  if (!fact.recordKey) return isCategoryActive(fact.category, activeStep);
  return focusedRecordKeys.has(fact.recordKey);
}

const MERGE_GAP = 4;

function rectsOverlapOrTouch(a: Rect, b: Rect, gap: number): boolean {
  return !(
    a.left - gap > b.left + b.width ||
    b.left - gap > a.left + a.width ||
    a.top - gap > b.top + b.height ||
    b.top - gap > a.top + a.height
  );
}

function unionRect(a: Rect, b: Rect): Rect {
  const left = Math.min(a.left, b.left);
  const top = Math.min(a.top, b.top);
  const right = Math.max(a.left + a.width, b.left + b.width);
  const bottom = Math.max(a.top + a.height, b.top + b.height);
  return { left, top, width: right - left, height: bottom - top };
}

// Marcações próximas (a mesma frase costuma cair em vários spans do pdf.js,
// e fatos diferentes podem compartilhar trecho) são unidas num só retângulo,
// em vez de empilhar destaques translúcidos sobrepostos.
function mergeOverlappingRects(rects: Rect[]): Rect[] {
  const merged = rects.map((rect) => ({ ...rect }));
  let changed = true;
  while (changed) {
    changed = false;
    for (let i = 0; i < merged.length && !changed; i++) {
      for (let j = i + 1; j < merged.length; j++) {
        if (rectsOverlapOrTouch(merged[i], merged[j], MERGE_GAP)) {
          merged[i] = unionRect(merged[i], merged[j]);
          merged.splice(j, 1);
          changed = true;
          break;
        }
      }
    }
  }
  return merged;
}

function clusterRects(rects: Rect[]): Rect[] {
  const sorted = [...rects].sort((a, b) => a.top - b.top);
  const clusters: Rect[] = [];

  for (const rect of sorted) {
    const last = clusters.at(-1);
    if (last && rect.top <= last.top + last.height + CLUSTER_GAP) {
      const top = Math.min(last.top, rect.top);
      const left = Math.min(last.left, rect.left);
      const bottom = Math.max(last.top + last.height, rect.top + rect.height);
      const right = Math.max(last.left + last.width, rect.left + rect.width);
      clusters[clusters.length - 1] = {
        top,
        left,
        width: right - left,
        height: bottom - top,
      };
      continue;
    }
    clusters.push({ ...rect });
  }

  return clusters;
}

function useContainerWidth<T extends HTMLElement>() {
  const ref = useRef<T>(null);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setWidth(entry.contentRect.width);
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return [ref, width] as const;
}

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

  const factsByPage = useMemo(() => {
    const map = new Map<number, EvidenceFact[]>();
    for (const fact of facts) {
      const list = map.get(fact.pageNumber) ?? [];
      list.push(fact);
      map.set(fact.pageNumber, list);
    }
    return map;
  }, [facts]);

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

function PdfHighlightPage({
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
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [marks, setMarks] = useState<Mark[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [matchedFactIds, setMatchedFactIds] = useState<Set<string>>(new Set());
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
      setMarks([]);
      setGroups([]);
      setMatchedFactIds(new Set());
      return;
    }

    // Páginas escaneadas (OCR) não têm camada de texto selecionável no pdf.js:
    // não há span algum para ancorar um destaque, mesmo que o fato exista.
    const hasText = textLayer.querySelector("span") !== null;
    setPageHasText(hasText);
    if (!hasText || facts.length === 0) {
      setMarks([]);
      setGroups([]);
      setMatchedFactIds(new Set());
      return;
    }

    const wrapperRect = wrapper.getBoundingClientRect();
    const matched = new Set<string>();
    const rectsByCluster = new Map<
      string,
      { category: string; focused: boolean; rects: Rect[] }
    >();
    const rectsByMark = new Map<
      string,
      { category: string; focused: boolean; rects: Rect[] }
    >();

    for (const fact of facts) {
      const spans = findEvidenceSpans(textLayer, [fact.excerpt, fact.value]);
      if (spans.length === 0) continue;
      matched.add(fact.id);
      const focused = isFactFocused(fact, activeStep, focusedRecordKeys);
      const clusterKey = fact.recordKey ?? fact.category;
      // Facts da mesma categoria e estado de foco compartilham a mesma cor,
      // então seus spans podem ser unidos sem perder informação visual.
      const markKey = `${fact.category}|${focused}`;

      for (const span of spans) {
        const spanRect = span.getBoundingClientRect();
        const rect: Rect = {
          top: spanRect.top - wrapperRect.top,
          left: spanRect.left - wrapperRect.left,
          width: spanRect.width,
          height: spanRect.height,
        };

        const clusterEntry = rectsByCluster.get(clusterKey) ?? {
          category: fact.category,
          focused,
          rects: [],
        };
        clusterEntry.rects.push(rect);
        // Se qualquer fato do registro estiver em foco, o bloco inteiro acompanha.
        clusterEntry.focused = clusterEntry.focused || focused;
        rectsByCluster.set(clusterKey, clusterEntry);

        const markEntry = rectsByMark.get(markKey) ?? {
          category: fact.category,
          focused,
          rects: [],
        };
        markEntry.rects.push(rect);
        rectsByMark.set(markKey, markEntry);
      }
    }

    const nextGroups: Group[] = [];
    for (const [clusterKey, entry] of rectsByCluster) {
      for (const cluster of clusterRects(entry.rects)) {
        nextGroups.push({
          id: `${clusterKey}-${nextGroups.length}`,
          category: entry.category,
          focused: entry.focused,
          rect: {
            top: cluster.top - GROUP_PADDING,
            left: cluster.left - GROUP_PADDING,
            width: cluster.width + GROUP_PADDING * 2,
            height: cluster.height + GROUP_PADDING * 2,
          },
        });
      }
    }

    const nextMarks: Mark[] = [];
    for (const [markKey, entry] of rectsByMark) {
      for (const rect of mergeOverlappingRects(entry.rects)) {
        nextMarks.push({
          id: `${markKey}-${nextMarks.length}`,
          category: entry.category,
          focused: entry.focused,
          rect,
        });
      }
    }

    setMarks(nextMarks);
    setGroups(nextGroups);
    setMatchedFactIds(matched);
  }, [facts, activeStep, focusedRecordKeys]);

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
          {matchedFactIds.size}/{facts.length} marcados
        </span>
      )}

      {groups.map((group) => (
        <div
          key={group.id}
          aria-hidden="true"
          className={`pointer-events-none absolute select-none rounded-xl transition-all duration-300 ${
            group.focused
              ? EVIDENCE_TONE.box.focused
              : EVIDENCE_TONE.box.neutral
          }`}
          style={{
            top: group.rect.top,
            left: group.rect.left,
            width: group.rect.width,
            height: group.rect.height,
          }}
        >
          <span
            className={`pointer-events-none absolute -top-2.5 right-3 inline-flex select-none items-center gap-1 whitespace-nowrap rounded-md px-2 py-0.5 text-[10px] font-semibold tracking-[0.3px] shadow-xs ${
              group.focused
                ? EVIDENCE_TONE.badge.focused
                : EVIDENCE_TONE.badge.neutral
            }`}
          >
            {categoryLabel(group.category)}{" "}
            {group.focused ? "• Em foco" : "mapeado"}
          </span>
        </div>
      ))}

      {marks.map((mark) => (
        <span
          key={mark.id}
          aria-hidden="true"
          className={`pointer-events-none absolute select-none rounded-[3px] transition-all duration-300 ${
            mark.focused
              ? EVIDENCE_TONE.mark.focused
              : EVIDENCE_TONE.mark.neutral
          }`}
          style={{
            top: mark.rect.top - 1,
            left: mark.rect.left - 2,
            width: mark.rect.width + 4,
            height: mark.rect.height + 2,
          }}
        />
      ))}
    </div>
  );
}
