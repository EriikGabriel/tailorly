export interface TextChange {
  from: number;
  to: number;
  insert: string;
}

export interface TextEdit {
  changes: TextChange[];
  /** Seleção depois das alterações, em posições do documento novo. */
  selection: { anchor: number; head: number };
}

/** Posição depois de aplicar as alterações (que não se sobrepõem), para manter a seleção no lugar. */
function mapPosition(position: number, changes: TextChange[]): number {
  let shift = 0;
  for (const change of changes) {
    if (change.to <= position)
      shift += change.insert.length - (change.to - change.from);
    else if (change.from < position)
      return change.from + shift + change.insert.length;
  }
  return position + shift;
}

/**
 * Envolve a seleção com `open`/`close` (negrito, itálico, código). Se ela já estiver envolvida,
 * remove os delimitadores. Sem seleção, insere `placeholder` já selecionado.
 */
export function wrapSelection(
  doc: string,
  from: number,
  to: number,
  open: string,
  close: string,
  placeholder: string,
): TextEdit {
  const selected = doc.slice(from, to);
  if (
    from >= open.length &&
    doc.slice(from - open.length, from) === open &&
    doc.slice(to, to + close.length) === close
  ) {
    return {
      changes: [
        { from: from - open.length, to: from, insert: "" },
        { from: to, to: to + close.length, insert: "" },
      ],
      selection: { anchor: from - open.length, head: to - open.length },
    };
  }
  if (
    selected.length >= open.length + close.length &&
    selected.startsWith(open) &&
    selected.endsWith(close)
  ) {
    const inner = selected.slice(open.length, selected.length - close.length);
    return {
      changes: [{ from, to, insert: inner }],
      selection: { anchor: from, head: from + inner.length },
    };
  }
  const text = selected === "" ? placeholder : selected;
  return {
    changes: [{ from, to, insert: open + text + close }],
    selection: {
      anchor: from + open.length,
      head: from + open.length + text.length,
    },
  };
}

/** Liga ou desliga um prefixo (`- `, `+ `, `// `) em todas as linhas tocadas pela seleção. */
export function toggleLinePrefix(
  doc: string,
  from: number,
  to: number,
  prefix: string,
): TextEdit {
  const lastPosition = to > from && doc[to - 1] === "\n" ? to - 1 : to;
  const firstStart = doc.lastIndexOf("\n", from - 1) + 1;
  const starts: number[] = [];
  let cursor = firstStart;
  while (cursor <= lastPosition) {
    starts.push(cursor);
    const next = doc.indexOf("\n", cursor);
    if (next === -1) break;
    cursor = next + 1;
  }
  const lineText = (start: number) =>
    doc.slice(
      start,
      doc.indexOf("\n", start) === -1 ? doc.length : doc.indexOf("\n", start),
    );
  const filled = starts.filter((start) => lineText(start).trim() !== "");
  const targets = filled.length > 0 ? filled : starts;
  const allPrefixed =
    filled.length > 0 &&
    filled.every((start) => lineText(start).startsWith(prefix));

  const changes: TextChange[] = targets.map((start) =>
    allPrefixed
      ? { from: start, to: start + prefix.length, insert: "" }
      : { from: start, to: start, insert: prefix },
  );
  return {
    changes,
    selection: {
      anchor: mapPosition(from, changes),
      head: mapPosition(to, changes),
    },
  };
}

/** Insere `#link("https://")[texto]`, com o endereço selecionado para ser digitado. */
export function insertLink(doc: string, from: number, to: number): TextEdit {
  const label = doc.slice(from, to) || "texto";
  const head = '#link("';
  const url = "https://";
  const insert = `${head}${url}")[${label}]`;
  return {
    changes: [{ from, to, insert }],
    selection: {
      anchor: from + head.length,
      head: from + head.length + url.length,
    },
  };
}
