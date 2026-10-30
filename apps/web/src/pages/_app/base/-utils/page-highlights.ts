import type { ReviewStepId } from "@@types/review-model";
import { findEvidenceSpans } from "@/utils/pdf-text-match";
import { GROUP_PADDING } from "../-constants/evidence";
import type {
  EvidenceFact,
  HighlightGroup,
  HighlightMark,
  PageHighlights,
  Rect,
} from "../-types/evidence";
import { isFactFocused } from "./evidence-category";
import { clusterRects, mergeOverlappingRects } from "./rect-geometry";

type RectBucket = { category: string; focused: boolean; rects: Rect[] };

type PageHighlightsInput = {
  facts: EvidenceFact[];
  /** Camada de texto do pdf.js, onde os trechos de evidência são procurados. */
  textLayer: Element;
  /** Retângulo da página na tela; as marcações são relativas a ele. */
  wrapperRect: DOMRect;
  activeStep: ReviewStepId;
  focusedRecordKeys: ReadonlySet<string>;
};

function relativeRect(span: Element, wrapperRect: DOMRect): Rect {
  const spanRect = span.getBoundingClientRect();
  return {
    top: spanRect.top - wrapperRect.top,
    left: spanRect.left - wrapperRect.left,
    width: spanRect.width,
    height: spanRect.height,
  };
}

/**
 * Localiza o trecho de cada fato na camada de texto e calcula os destaques da página:
 * `groups` envolvem o bloco inteiro de um registro e `marks` cobrem o texto de cada fato.
 */
export function computePageHighlights({
  facts,
  textLayer,
  wrapperRect,
  activeStep,
  focusedRecordKeys,
}: PageHighlightsInput): PageHighlights {
  const matchedFactIds = new Set<string>();
  const rectsByCluster = new Map<string, RectBucket>();
  const rectsByMark = new Map<string, RectBucket>();

  for (const fact of facts) {
    const spans = findEvidenceSpans(textLayer, [fact.excerpt, fact.value]);
    if (spans.length === 0) continue;
    matchedFactIds.add(fact.id);
    const focused = isFactFocused(fact, activeStep, focusedRecordKeys);
    const clusterKey = fact.recordKey ?? fact.category;
    // Facts da mesma categoria e estado de foco compartilham a mesma cor,
    // então seus spans podem ser unidos sem perder informação visual.
    const markKey = `${fact.category}|${focused}`;

    for (const span of spans) {
      const rect = relativeRect(span, wrapperRect);

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

  const groups: HighlightGroup[] = [];
  for (const [clusterKey, entry] of rectsByCluster) {
    for (const cluster of clusterRects(entry.rects)) {
      groups.push({
        id: `${clusterKey}-${groups.length}`,
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

  const marks: HighlightMark[] = [];
  for (const [markKey, entry] of rectsByMark) {
    for (const rect of mergeOverlappingRects(entry.rects)) {
      marks.push({
        id: `${markKey}-${marks.length}`,
        category: entry.category,
        focused: entry.focused,
        rect,
      });
    }
  }

  return { marks, groups, matchedFactIds };
}
