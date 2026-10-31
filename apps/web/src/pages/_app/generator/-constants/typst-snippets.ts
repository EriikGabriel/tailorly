/** biome-ignore-all lint/suspicious/noTemplateCurlyInString: os snippets do CodeMirror usam campos ${1:padrão} de propósito */

export interface TypstSnippet {
  label: string;
  /** Snippet do CodeMirror: campos `${1:padrão}` são percorridos com Tab. */
  template: string;
}

export interface TypstSnippetGroup {
  title: string;
  snippets: TypstSnippet[];
}

/** Trechos prontos do menu "Inserir", pensados para ajustar o visual de um currículo. */
export const TYPST_SNIPPET_GROUPS: TypstSnippetGroup[] = [
  {
    title: "Estilo do documento",
    snippets: [
      {
        label: "Cor de destaque",
        template: '#let accent = rgb("${1:#0f4c5c}")\n',
      },
      {
        label: "Fonte e tamanho",
        template:
          '#set text(font: ("${1:Libertinus Serif}", "New Computer Modern"), size: ${2:10.5}pt)\n',
      },
      {
        label: "Papel e margens",
        template:
          '#set page(paper: "a4", margin: (x: ${1:1.8cm}, y: ${2:1.6cm}))\n',
      },
      {
        label: "Espaçamento de parágrafo",
        template: "#set par(leading: ${1:0.55em}, spacing: ${2:0.7em})\n",
      },
      {
        label: "Estilo dos títulos de seção",
        template:
          '#show heading.where(level: ${1:2}): it => block(above: ${2:1.1em}, below: ${3:0.5em}, width: 100%)[\n  #text(size: 1.1em, weight: "bold", it.body)\n  #v(-0.45em)\n  #line(length: 100%, stroke: ${4:0.6pt})\n]\n',
      },
    ],
  },
  {
    title: "Elementos",
    snippets: [
      {
        label: "Linha horizontal",
        template: "#line(length: ${1:100%}, stroke: ${2:0.6pt})",
      },
      { label: "Espaço flexível (empurra à direita)", template: "#h(1fr)" },
      { label: "Espaço vertical", template: "#v(${1:0.5em})" },
      {
        label: "Duas colunas",
        template:
          "#grid(columns: (${1:1fr, 1fr}), gutter: ${2:1em},\n  [${3}],\n  [${4}],\n)",
      },
      {
        label: "Bloco com fundo",
        template:
          "#block(fill: ${1:luma(245)}, inset: ${2:0.6em}, radius: ${3:3pt}, width: 100%)[${4}]",
      },
      { label: "Quebra de página", template: "#pagebreak()" },
    ],
  },
];
