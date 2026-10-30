import { CLUSTER_GAP, MERGE_GAP } from "../-constants/evidence";
import type { Rect } from "../-types/evidence";

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

/**
 * Marcações próximas (a mesma frase costuma cair em vários spans do pdf.js,
 * e fatos diferentes podem compartilhar trecho) são unidas num só retângulo,
 * em vez de empilhar destaques translúcidos sobrepostos.
 */
export function mergeOverlappingRects(rects: Rect[]): Rect[] {
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

/** Agrupa retângulos verticalmente próximos (ex.: linhas de um mesmo cargo) em blocos. */
export function clusterRects(rects: Rect[]): Rect[] {
  const sorted = [...rects].sort((a, b) => a.top - b.top);
  const clusters: Rect[] = [];

  for (const rect of sorted) {
    const last = clusters.at(-1);
    if (last && rect.top <= last.top + last.height + CLUSTER_GAP) {
      clusters[clusters.length - 1] = unionRect(last, rect);
      continue;
    }
    clusters.push({ ...rect });
  }

  return clusters;
}
