// Template "classic" v1

// Funções e variáveis reaproveitadas pelo layout
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = false // tailorly:compact

// Página e tipografia
#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.5cm, y: 1.3cm) } else { (x: 1.9cm, y: 1.7cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern", "DejaVu Serif"), size: 10.5pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.45em } else { 0.55em }, spacing: if compact { 0.5em } else { 0.7em })
#set list(indent: 0.4em, body-indent: 0.5em, tight: false, spacing: if compact { 0.4em } else { 0.6em })

// Títulos de seção: negrito com filete abaixo
#show heading.where(level: 2): it => block(above: if compact { 1.1em } else { 1.6em }, below: 0.8em, width: 100%)[
  #text(size: 1.1em, weight: "bold", it.body)
  #v(-0.45em)
  #line(length: 100%, stroke: 0.6pt + luma(60))
]

// Conteúdo aprovado
// tailorly:content
