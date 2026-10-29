// Template "classic", versão 1: uma coluna, texto real e cabeçalhos de seção padrão, que é o que os
// sistemas de triagem (ATS) leem melhor. Sem tabelas, imagens, ícones nem texto em caixas.
//
// O conteúdo aprovado é escrito direto no corpo, como texto, no lugar do marcador abaixo (ver
// TypstTemplateMaterializer); fica em variável só o que o layout precisa reaproveitar em vários
// pontos. Pode editar à vontade o visual das linhas acima do marcador; não escreva aqui nenhum
// fato pessoal ou texto de exemplo.
#let lines(text) = text.split("\n").map(line => [#line]).join(linebreak())
#let compact = false // tailorly:compact

#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.5cm, y: 1.3cm) } else { (x: 1.9cm, y: 1.7cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern", "DejaVu Serif"), size: 10.5pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.45em } else { 0.55em }, spacing: if compact { 0.5em } else { 0.7em })
#set list(indent: 0.4em, body-indent: 0.5em)

#show heading.where(level: 2): it => block(above: if compact { 0.8em } else { 1.1em }, below: 0.5em, width: 100%)[
  #text(size: 1.1em, weight: "bold", it.body)
  #v(-0.45em)
  #line(length: 100%, stroke: 0.6pt + luma(60))
]

// tailorly:content
