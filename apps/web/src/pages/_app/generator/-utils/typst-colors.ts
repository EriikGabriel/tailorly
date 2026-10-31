export interface ColorLiteral {
  /** Início da expressão `rgb(...)`, onde a amostra de cor é desenhada. */
  exprFrom: number;
  /** Intervalo do texto hexadecimal (com `#`) dentro do documento. */
  from: number;
  to: number;
  /** Cor em `#rrggbb` minúsculo, sem transparência, pronta para um seletor de cor. */
  hex: string;
  /** Texto original, com o formato e a transparência que a pessoa escreveu. */
  original: string;
}

const RGB_LITERAL = /rgb\(\s*"(#[0-9a-fA-F]{3,8})"\s*\)/g;
const VALID_LENGTHS = new Set([4, 5, 7, 9]); // # + 3, 4, 6 ou 8 dígitos

/** Cores `rgb("#...")` do texto, para mostrar uma amostra ao lado de cada uma. */
export function findColorLiterals(text: string, offset = 0): ColorLiteral[] {
  const found: ColorLiteral[] = [];
  for (const match of text.matchAll(RGB_LITERAL)) {
    const original = match[1];
    if (!VALID_LENGTHS.has(original.length)) continue;
    const from = offset + (match.index ?? 0) + match[0].indexOf("#");
    found.push({
      exprFrom: offset + (match.index ?? 0),
      from,
      to: from + original.length,
      hex: toSixDigitHex(original),
      original,
    });
  }
  return found;
}

/** `#abc` e `#abcd` viram `#aabbcc`; `#rrggbbaa` perde a transparência. */
export function toSixDigitHex(original: string): string {
  const digits = original.slice(1).toLowerCase();
  if (digits.length === 3 || digits.length === 4) {
    return `#${[...digits.slice(0, 3)].map((digit) => digit + digit).join("")}`;
  }
  return `#${digits.slice(0, 6)}`;
}

/** Texto para substituir o original por uma cor escolhida, mantendo a transparência se havia. */
export function applyPickedColor(original: string, picked: string): string {
  const digits = original.slice(1);
  const alpha =
    digits.length === 8
      ? digits.slice(6)
      : digits.length === 4
        ? digits[3] + digits[3]
        : "";
  return picked.toLowerCase() + alpha;
}
