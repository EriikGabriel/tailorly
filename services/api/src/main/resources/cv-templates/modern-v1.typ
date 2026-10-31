// Template "modern", versão 1: uma coluna com acento de cor nos cabeçalhos de seção. Continua
// ATS-friendly: texto real e selecionável, sem tabelas, imagens, ícones nem texto em caixas. A cor
// só aparece nos títulos de seção; o corpo fica em preto para leitura e extração.
//
// O conteúdo aprovado é escrito no lugar do marcador abaixo (ver TypstTemplateMaterializer). Pode
// editar o visual das linhas acima do marcador; não escreva aqui nenhum fato pessoal ou exemplo.
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = false // tailorly:compact
#let accent = rgb("#0f4c5c")

#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.5cm, y: 1.3cm) } else { (x: 1.8cm, y: 1.6cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern"), size: 10pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.45em } else { 0.6em }, spacing: if compact { 0.5em } else { 0.75em })
#set list(indent: 0.4em, body-indent: 0.5em)

#show heading.where(level: 2): it => block(above: if compact { 0.9em } else { 1.3em }, below: 0.6em, width: 100%)[
  #text(size: 0.95em, weight: "bold", fill: accent, tracking: 0.06em, it.body)
  #v(-0.4em)
  #line(length: 100%, stroke: 1.1pt + accent)
]

// tailorly:content
