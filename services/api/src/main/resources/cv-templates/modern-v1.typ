// Template "modern" v1

// Funções e variáveis reaproveitadas pelo layout
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = false // tailorly:compact
#let accent = rgb("#0f4c5c")

// Página e tipografia
#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.5cm, y: 1.3cm) } else { (x: 1.8cm, y: 1.6cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern"), size: 10pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.45em } else { 0.6em }, spacing: if compact { 0.5em } else { 0.75em })
#set list(indent: 0.4em, body-indent: 0.5em, tight: false, spacing: if compact { 0.4em } else { 0.6em })

// Títulos de seção: negrito com acento de cor e filete
#show heading.where(level: 2): it => block(above: if compact { 1.2em } else { 1.7em }, below: 0.8em, width: 100%)[
  #text(size: 0.95em, weight: "bold", fill: accent, tracking: 0.06em, it.body)
  #v(-0.4em)
  #line(length: 100%, stroke: 1.1pt + accent)
]

// Conteúdo aprovado
// tailorly:content
