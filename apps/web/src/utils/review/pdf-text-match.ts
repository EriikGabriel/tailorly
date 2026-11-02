type Transform = (value: string) => string;

const identity: Transform = (value) => value;
const lowerCase: Transform = (value) => value.toLowerCase();
const foldAccentsAndSpaces: Transform = (value) =>
  value.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/\s+/g, " ");

// Da mais estrita para a mais tolerante: tentamos primeiro uma correspondência
// exata (mais confiável) antes de relaxar maiúsculas, acentos e espaçamento —
// diferenças comuns entre o texto extraído pelo pdf.js e a evidência salva.
const TRANSFORMS: Transform[] = [identity, lowerCase, foldAccentsAndSpaces];

type SpanIndex = {
  raw: string;
  ranges: { start: number; end: number; span: HTMLElement }[];
};

function buildIndex(spans: HTMLElement[], transform: Transform): SpanIndex {
  let raw = "";
  const ranges: SpanIndex["ranges"] = [];
  for (const span of spans) {
    const text = transform(span.textContent ?? "");
    if (!text) continue;
    ranges.push({ start: raw.length, end: raw.length + text.length, span });
    raw += text;
  }
  return { raw, ranges };
}

function spansForRange(
  index: SpanIndex,
  start: number,
  end: number,
): HTMLElement[] {
  return index.ranges
    .filter((item) => item.start < end && item.end > start)
    .map((item) => item.span);
}

/**
 * Localiza, dentro da camada de texto do pdf.js já renderizada, os spans que
 * correspondem a um dos textos candidatos (geralmente o valor literal do fato
 * e, como alternativa, o trecho de evidência mais longo). Tenta várias
 * tolerâncias — exata, sem diferenciar maiúsculas, sem acentos/espaçamento —
 * e, na ausência de qualquer correspondência de frase, cai para a maior
 * palavra em comum como último recurso, para que o ponto de extração ainda
 * fique localizável no documento.
 */
export function findEvidenceSpans(
  container: Element,
  candidates: readonly (string | null | undefined)[],
): HTMLElement[] {
  const needles = candidates
    .map((candidate) => candidate?.trim())
    .filter((candidate): candidate is string => Boolean(candidate));
  if (needles.length === 0) return [];

  // pdf.js agrupa alguns trechos em spans ".markedContent" (display:contents,
  // sem geometria própria) que envolvem os spans de texto reais. Sem filtrar,
  // o texto do agrupador entra duplicado no índice e a correspondência pode
  // "vencer" num elemento invisível em vez do span real que queremos marcar.
  const spans = Array.from(
    container.querySelectorAll<HTMLElement>("span"),
  ).filter((span) => span.childElementCount === 0);
  if (spans.length === 0) return [];

  for (const transform of TRANSFORMS) {
    const index = buildIndex(spans, transform);
    for (const needle of needles) {
      const term = transform(needle);
      if (!term) continue;
      const at = index.raw.indexOf(term);
      if (at === -1) continue;
      const spans = spansForRange(index, at, at + term.length);
      if (spans.length > 0) return spans;
    }
  }

  return findLongestWordMatch(spans, needles);
}

/** Último recurso: destaca a maior palavra do trecho de evidência encontrada no texto da página. */
function findLongestWordMatch(
  spans: HTMLElement[],
  needles: readonly string[],
): HTMLElement[] {
  const index = buildIndex(spans, foldAccentsAndSpaces);
  const words = needles
    .flatMap((needle) => foldAccentsAndSpaces(needle).split(/\s+/))
    .filter((word) => word.length >= 4)
    .sort((a, b) => b.length - a.length);

  for (const word of words) {
    const at = index.raw.indexOf(word);
    if (at === -1) continue;
    const spans = spansForRange(index, at, at + word.length);
    if (spans.length > 0) return spans;
  }

  return [];
}
