// Template "classic", versão 1: uma coluna, texto real e cabeçalhos de seção padrão, que é o que os
// sistemas de triagem (ATS) leem melhor. Sem tabelas, imagens, ícones nem texto em caixas.
//
// Só formata: todo texto vem de data.json, montado a partir do snapshot aprovado do Ground Truth.
// Não escreva aqui nenhum fato pessoal ou texto de exemplo. Pode editar à vontade o visual; a
// estrutura de `cv` é:
//   cv.name, cv.headline, cv.summary                       (texto ou null)
//   cv.contacts[]  { text, url }                           (url pode ser null)
//   cv.sections[]  { key, heading, compact, entries[] }
//   entry          { id, title, subtitle, meta, period, details[], link }
//   cv.layout      { density: "normal" | "compact", fontSize }
#let cv = json("data.json")

#let or-none(value) = if value == none or value == "" { none } else { value }
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = cv.layout.density == "compact"

#set document(title: if or-none(cv.name) != none { cv.name } else { "Currículo" })
#set page(paper: "a4", margin: if compact { (x: 1.5cm, y: 1.3cm) } else { (x: 1.9cm, y: 1.7cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern", "DejaVu Serif"), size: cv.layout.fontSize * 1pt, lang: "pt")
#set par(justify: false, leading: if compact { 0.45em } else { 0.55em }, spacing: if compact { 0.5em } else { 0.7em })
#set list(indent: 0.4em, body-indent: 0.5em)

#show heading.where(level: 2): it => block(above: if compact { 0.8em } else { 1.1em }, below: 0.5em, width: 100%)[
  #text(size: 1.1em, weight: "bold", it.body)
  #v(-0.45em)
  #line(length: 100%, stroke: 0.6pt + luma(60))
]

#if or-none(cv.name) != none [
  #align(center, text(size: 2em, weight: "bold", cv.name))
]
#if or-none(cv.headline) != none [
  #align(center, text(size: 1.1em, cv.headline))
]
#if cv.contacts.len() > 0 [
  #align(center, text(size: 0.92em)[
    #cv.contacts.map(contact => if or-none(contact.url) != none { link(contact.url)[#contact.text] } else { [#contact.text] }).join([ · ])
  ])
]

#if or-none(cv.summary) != none [
  == Resumo
  #lines(cv.summary)
]

#for section in cv.sections [
  == #section.heading
  #if section.compact [
    #for entry in section.entries [
      - #entry.title#if or-none(entry.subtitle) != none [ — #entry.subtitle]
    ]
  ] else [
    #for entry in section.entries [
      #block(breakable: false, below: if compact { 0.6em } else { 0.9em })[
        *#entry.title*#if or-none(entry.subtitle) != none [ — #entry.subtitle]#if or-none(entry.period) != none [ #h(1fr) #text(size: 0.92em, entry.period)] \
        #if or-none(entry.meta) != none [
          #text(size: 0.92em, style: "italic", entry.meta) \
        ]
        #for detail in entry.details [
          #lines(detail) \
        ]
        #if or-none(entry.link) != none [
          #text(size: 0.92em, link(entry.link)[#entry.link])
        ]
      ]
    ]
  ]
]
