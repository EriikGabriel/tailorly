export type EvidenceFact = {
  id: string;
  category: string;
  value: string;
  excerpt: string;
  pageNumber: number;
  /**
   * Identifica a entrada repetível (ex.: "Experiência 2") a que o fato
   * pertence, no mesmo formato usado por `expandedRecords` do rascunho de
   * revisão (`${recordId}-${index}`). `null` para categorias sem repetição
   * (ex.: identidade), que não têm um registro específico para focar.
   */
  recordKey: string | null;
};

/** Retângulo em px, relativo ao topo/esquerda da página renderizada. */
export type Rect = { top: number; left: number; width: number; height: number };

/** Destaque sobre o texto de um fato (uma ou mais frases unidas). */
export type HighlightMark = {
  id: string;
  category: string;
  focused: boolean;
  rect: Rect;
};

/** Caixa que envolve todas as marcações de um mesmo registro. */
export type HighlightGroup = {
  id: string;
  category: string;
  focused: boolean;
  rect: Rect;
};

/** Resultado do cálculo de destaques de uma página. */
export type PageHighlights = {
  marks: HighlightMark[];
  groups: HighlightGroup[];
  matchedFactIds: Set<string>;
};
