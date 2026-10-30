const INDEXED_FIELD_KEY = /^(\d+):(.*)$/;

/**
 * Fatos de categorias repetíveis usam chaves `"{index}:campo"` (ex.: `"0:role"`); a identidade é
 * plana e não tem índice. Sem prefixo, o fato pertence à primeira entrada (índice 0).
 */
export function parseFactFieldKey(fieldKey: string): {
  index: number;
  key: string;
} {
  const match = INDEXED_FIELD_KEY.exec(fieldKey);
  return match
    ? { index: Number(match[1]), key: match[2] }
    : { index: 0, key: fieldKey };
}

/** Inverso de {@link parseFactFieldKey} para entradas repetíveis. */
export function indexedFieldKey(index: number, key: string): string {
  return `${index}:${key}`;
}
