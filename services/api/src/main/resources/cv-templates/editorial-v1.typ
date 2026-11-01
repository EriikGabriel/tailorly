// Template "editorial" v1

// Funções e variáveis reaproveitadas pelo layout
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = false // tailorly:compact
#let accent = rgb("#5b3a1a")

// Página e tipografia
#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.6cm, y: 1.3cm) } else { (x: 2cm, y: 1.8cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern", "DejaVu Serif"), size: 10.5pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.45em } else { 0.6em }, spacing: if compact { 0.5em } else { 0.75em })
#set list(indent: 0.4em, body-indent: 0.5em, tight: false, spacing: if compact { 0.4em } else { 0.6em })

// Títulos de seção: itálico com filete fino
#show heading.where(level: 2): it => block(above: if compact { 1.2em } else { 1.8em }, below: 0.8em, width: 100%)[
  #text(size: 1.15em, style: "italic", fill: accent, it.body)
  #v(-0.35em)
  #line(length: 100%, stroke: 0.4pt + accent)
]

// Conteúdo aprovado
// tailorly:content
