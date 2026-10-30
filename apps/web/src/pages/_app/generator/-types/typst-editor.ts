/** Posição do cursor e contagens do documento, mostradas na barra de status do editor. */
export interface CursorInfo {
  line: number;
  column: number;
  characters: number;
  words: number;
}
