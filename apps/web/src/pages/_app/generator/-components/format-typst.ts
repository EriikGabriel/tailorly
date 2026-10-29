type TokenType =
  | "open"
  | "close"
  | "comma"
  | "colon"
  | "string"
  | "comment"
  | "text";

interface Token {
  type: TokenType;
  value: string;
  hadSpace: boolean;
}

const MATCHING_CLOSE: Record<string, string> = { "(": ")", "[": "]", "{": "}" };
const DELIMITERS = ' \t\n\r([{)]},:"';

function tokenize(source: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;
  const n = source.length;
  let hadSpace = false;

  while (i < n) {
    const ch = source[i];

    if (ch === " " || ch === "\t" || ch === "\n" || ch === "\r") {
      hadSpace = true;
      i++;
      continue;
    }

    if (ch === "/" && source[i + 1] === "/") {
      const start = i;
      while (i < n && source[i] !== "\n") i++;
      tokens.push({ type: "comment", value: source.slice(start, i), hadSpace });
      hadSpace = false;
      continue;
    }

    if (ch === "/" && source[i + 1] === "*") {
      const start = i;
      i += 2;
      while (i < n && !(source[i] === "*" && source[i + 1] === "/")) i++;
      i = Math.min(n, i + 2);
      tokens.push({ type: "comment", value: source.slice(start, i), hadSpace });
      hadSpace = false;
      continue;
    }

    if (ch === '"') {
      const start = i;
      i++;
      while (i < n) {
        if (source[i] === "\\") {
          i += 2;
          continue;
        }
        if (source[i] === '"') {
          i++;
          break;
        }
        i++;
      }
      tokens.push({ type: "string", value: source.slice(start, i), hadSpace });
      hadSpace = false;
      continue;
    }

    if (ch === "(" || ch === "[" || ch === "{") {
      tokens.push({ type: "open", value: ch, hadSpace });
      hadSpace = false;
      i++;
      continue;
    }

    if (ch === ")" || ch === "]" || ch === "}") {
      tokens.push({ type: "close", value: ch, hadSpace });
      hadSpace = false;
      i++;
      continue;
    }

    if (ch === ",") {
      tokens.push({ type: "comma", value: ",", hadSpace });
      hadSpace = false;
      i++;
      continue;
    }

    if (ch === ":") {
      tokens.push({ type: "colon", value: ":", hadSpace });
      hadSpace = false;
      i++;
      continue;
    }

    const start = i;
    while (
      i < n &&
      !DELIMITERS.includes(source[i]) &&
      !(source[i] === "/" && (source[i + 1] === "/" || source[i + 1] === "*"))
    ) {
      i++;
    }
    if (i === start) {
      i++;
    }
    tokens.push({ type: "text", value: source.slice(start, i), hadSpace });
    hadSpace = false;
  }

  return tokens;
}

/**
 * Reindenta literais Typst estruturados em parênteses/colchetes (dicionários e arrays de
 * `#let nome = (...)`), uma linha por campo. Não é um formatador Typst completo: preserva
 * strings e comentários ao pé da letra e normaliza apenas o espaçamento entre tokens.
 */
export function formatTypstSource(source: string, indentUnit = "  "): string {
  const tokens = tokenize(source);
  let out = "";
  let depth = 0;
  let atLineStart = true;
  let lastWasOpen = false;
  let suppressSpace = false;

  function write(text: string) {
    out += text;
    atLineStart = false;
  }

  function newline() {
    out += `\n${indentUnit.repeat(depth)}`;
    atLineStart = true;
  }

  for (let idx = 0; idx < tokens.length; idx++) {
    const tok = tokens[idx];
    const next = tokens[idx + 1];

    if (tok.type === "open") {
      if (tok.hadSpace && !suppressSpace && out.length > 0 && !atLineStart) {
        write(" ");
      }
      suppressSpace = false;
      write(tok.value);
      if (
        next &&
        next.type === "close" &&
        MATCHING_CLOSE[tok.value] === next.value
      ) {
        lastWasOpen = true;
      } else {
        depth++;
        newline();
        lastWasOpen = false;
      }
      continue;
    }

    if (tok.type === "close") {
      if (lastWasOpen) {
        write(tok.value);
      } else {
        depth = Math.max(0, depth - 1);
        newline();
        write(tok.value);
      }
      lastWasOpen = false;
      continue;
    }

    if (tok.type === "comma") {
      write(",");
      if (next?.type !== "close") {
        newline();
      }
      lastWasOpen = false;
      continue;
    }

    if (tok.type === "colon") {
      write(": ");
      lastWasOpen = false;
      suppressSpace = true;
      continue;
    }

    if (tok.hadSpace && !suppressSpace && out.length > 0 && !atLineStart) {
      write(" ");
    }
    suppressSpace = false;
    write(tok.value);
    lastWasOpen = false;
  }

  return `${out.trimEnd()}\n`;
}

const DATA_LITERAL_PATTERN = /#let\s+cv\s*=\s*\(/;

function findMatchingParenEnd(source: string, openIndex: number): number {
  let depth = 0;
  let inString = false;
  for (let i = openIndex; i < source.length; i++) {
    const ch = source[i];
    if (inString) {
      if (ch === "\\") {
        i++;
        continue;
      }
      if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') {
      inString = true;
      continue;
    }
    if (ch === "(") depth++;
    else if (ch === ")") {
      depth--;
      if (depth === 0) return i;
    }
  }
  return -1;
}

/**
 * Reindenta só o literal de dados que o backend injeta em `#let cv = (...)` (ver
 * TypstTemplateMaterializer), deixando o resto do template Typst — markup, `#if`/`#for`, blocos de
 * conteúdo `[...]` — intocado. Reformatar o arquivo inteiro quebraria espaços significativos fora
 * do literal de dados.
 */
export function formatTypstTemplate(source: string, indentUnit = "  "): string {
  const match = DATA_LITERAL_PATTERN.exec(source);
  if (!match) return source;

  const openIndex = match.index + match[0].length - 1;
  const closeIndex = findMatchingParenEnd(source, openIndex);
  if (closeIndex === -1) return source;

  const before = source.slice(0, openIndex);
  const literal = source.slice(openIndex, closeIndex + 1);
  const after = source.slice(closeIndex + 1);

  return `${before}${formatTypstSource(literal, indentUnit).trimEnd()}${after}`;
}
