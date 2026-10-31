/** biome-ignore-all lint/suspicious/noTemplateCurlyInString: os snippets do CodeMirror usam campos ${1:padrão} de propósito */

export type CompletionKind =
  | "function"
  | "keyword"
  | "property"
  | "variable"
  | "constant";

export interface TypstCompletion {
  label: string;
  kind: CompletionKind;
  detail?: string;
  /** Snippet do CodeMirror (campos `${1:padrão}`); sem ele, o próprio `label` é inserido. */
  snippet?: string;
}

export interface CompletionResult {
  /** Quantos caracteres antes do cursor o completar substitui. */
  prefixLength: number;
  options: TypstCompletion[];
}

/**
 * Sugestões só do que o autocompletar da gramática (`codemirror-lang-typst`) não conhece: os marcadores
 * `// tailorly:` e as receitas de template deste projeto. Funções da biblioteca padrão, argumentos e
 * variáveis locais já vêm da gramática.
 */

/** Marcadores que o servidor substitui ao materializar o template (ver TypstTemplateMaterializer). */
const MARKERS: TypstCompletion[] = [
  {
    label: "compact",
    kind: "keyword",
    detail: "linha #let compact: densidade compacta",
  },
  {
    label: "title",
    kind: "keyword",
    detail: "linha #set document: título do PDF",
  },
  {
    label: "font-size",
    kind: "keyword",
    detail: "linha #set text: tamanho da fonte",
  },
  {
    label: "content",
    kind: "keyword",
    detail: "onde o conteúdo aprovado é escrito",
  },
];

/** Receitas prontas para ajustar o visual de um currículo; aparecem depois de `#`. */
const RECIPES: TypstCompletion[] = [
  {
    label: "set text",
    kind: "keyword",
    detail: "fonte, tamanho e cor",
    snippet:
      'set text(font: ("${1:Libertinus Serif}", "New Computer Modern"), size: ${2:10.5}pt)',
  },
  {
    label: "set page",
    kind: "keyword",
    detail: "papel e margens",
    snippet: 'set page(paper: "a4", margin: (x: ${1:1.8cm}, y: ${2:1.6cm}))',
  },
  {
    label: "set par",
    kind: "keyword",
    detail: "parágrafo: espaçamento e justificação",
    snippet:
      "set par(justify: ${1:false}, leading: ${2:0.55em}, spacing: ${3:0.7em})",
  },
  {
    label: "set list",
    kind: "keyword",
    detail: "marcadores de lista",
    snippet: "set list(indent: ${1:0.4em}, body-indent: ${2:0.5em})",
  },
  {
    label: "show heading",
    kind: "keyword",
    detail: "estilo dos títulos de seção",
    snippet:
      'show heading.where(level: ${1:2}): it => block(above: ${2:1.1em}, below: ${3:0.5em}, width: 100%)[\n  #text(size: 1.1em, weight: "bold", it.body)\n  #v(-0.45em)\n  #line(length: 100%, stroke: ${4:0.6pt})\n]',
  },
  {
    label: "let accent",
    kind: "keyword",
    detail: "cor de destaque reutilizável",
    snippet: 'let accent = rgb("${1:#0f4c5c}")',
  },
];

/** Dentro de uma string ou de um comentário na linha atual do cursor (a conta ignora `\"`). */
function insideStringOrComment(before: string): boolean {
  const line = before.slice(before.lastIndexOf("\n") + 1);
  let inString = false;
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (inString) {
      if (char === "\\") i++;
      else if (char === '"') inString = false;
    } else if (char === '"') inString = true;
    else if (char === "/" && line[i + 1] === "/") return true;
  }
  return inString;
}

function startingWith(
  options: TypstCompletion[],
  prefix: string,
): TypstCompletion[] {
  const lower = prefix.toLowerCase();
  return options.filter((option) =>
    option.label.toLowerCase().startsWith(lower),
  );
}

/** Sugestões deste projeto para o texto antes do cursor, ou `null` quando não há nenhuma. */
export function templateCompletions(before: string): CompletionResult | null {
  const marker = /\/\/\s*tailorly:([\w-]*)$/.exec(before);
  if (marker) {
    return {
      prefixLength: marker[1].length,
      options: startingWith(MARKERS, marker[1]),
    };
  }
  if (insideStringOrComment(before)) return null;

  const hash = /#([A-Za-z][\w.-]*)?$/.exec(before);
  if (!hash) return null;
  const prefix = hash[1] ?? "";
  const options = startingWith(RECIPES, prefix);
  return options.length > 0 ? { prefixLength: prefix.length, options } : null;
}
