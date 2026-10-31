/** Marcador que o servidor substitui pelo corpo do currículo (ver TypstTemplateMaterializer). */
export const CONTENT_MARKER = "// tailorly:content";

export type DiagnosticSeverity = "error" | "warning";

export interface ParsedDiagnostic {
  severity: DiagnosticSeverity;
  message: string;
  /** Posição no código compilado (1-based); ausente quando o compilador não informou. */
  line: number | null;
  column: number | null;
}

export interface EditorDiagnostic {
  from: number;
  to: number;
  severity: DiagnosticSeverity;
  message: string;
}

const HEADER = /^(error|warning):\s*(.*)$/;
const LOCATION = /^\s*┌─\s+.+?:(\d+):(\d+)\s*$/;
const HINT = /^\s*=\s*(hint|help):\s*(.*)$/;

/**
 * Lê a saída de erro do compilador Typst: uma linha `error:`/`warning:`, a localização em
 * `┌─ arquivo:linha:coluna` e dicas `= hint:`. As linhas com o trecho de código são ignoradas.
 */
export function parseCompilerOutput(lines: string[]): ParsedDiagnostic[] {
  const result: ParsedDiagnostic[] = [];
  let current: ParsedDiagnostic | null = null;
  for (const raw of lines) {
    const header = HEADER.exec(raw);
    if (header) {
      current = {
        severity: header[1] === "warning" ? "warning" : "error",
        message: header[2],
        line: null,
        column: null,
      };
      result.push(current);
      continue;
    }
    if (!current) continue;
    const location = LOCATION.exec(raw);
    if (location && current.line === null) {
      current.line = Number(location[1]);
      current.column = Number(location[2]);
      continue;
    }
    const hint = HINT.exec(raw);
    if (hint) current.message += ` (${hint[2]})`;
  }
  return result;
}

/** Número (1-based) da linha com o marcador de conteúdo, ou `null` se o template não o tem. */
export function findContentMarkerLine(source: string): number | null {
  const index = source
    .split("\n")
    .findIndex((line) => line.includes(CONTENT_MARKER));
  return index === -1 ? null : index + 1;
}

/**
 * Converte diagnósticos do compilador em marcas no editor. O servidor troca o marcador de conteúdo
 * pelo corpo do currículo, então só as linhas anteriores a ele coincidem com o código editável; as
 * demais ficam fora (`unmapped`) e aparecem só no painel de erros.
 */
export function toEditorDiagnostics(
  parsed: ParsedDiagnostic[],
  source: string,
): { marks: EditorDiagnostic[]; unmapped: number } {
  const lines = source.split("\n");
  const markerLine = findContentMarkerLine(source) ?? Number.POSITIVE_INFINITY;
  const starts: number[] = [];
  let offset = 0;
  for (const line of lines) {
    starts.push(offset);
    offset += line.length + 1;
  }

  const marks: EditorDiagnostic[] = [];
  let unmapped = 0;
  for (const item of parsed) {
    if (
      item.line === null ||
      item.line < 1 ||
      item.line > lines.length ||
      item.line >= markerLine
    ) {
      unmapped++;
      continue;
    }
    const text = lines[item.line - 1];
    const column = Math.min(Math.max((item.column ?? 1) - 1, 0), text.length);
    const word = /^[\w-]+|^./u.exec(text.slice(column));
    const from = starts[item.line - 1] + column;
    const to = word ? from + word[0].length : from;
    marks.push({
      from,
      to: Math.max(to, Math.min(from + 1, starts[item.line - 1] + text.length)),
      severity: item.severity,
      message: item.message,
    });
  }
  return { marks, unmapped };
}
