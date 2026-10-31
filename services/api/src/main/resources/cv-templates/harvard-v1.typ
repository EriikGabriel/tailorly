// Template "harvard", versão 1: inspirado no formato de currículo recomendado pela Harvard (Office of
// Career Services): uma coluna, serifado, nome centralizado, seções em versalete com filete, marcadores
// nas descrições e datas alinhadas à direita. Continua ATS-friendly: texto real e selecionável, sem
// tabelas, imagens, ícones nem texto em caixas. Os marcadores são grades de duas colunas, sem tabela
// semântica; o texto de cada linha continua extraível.
//
// O conteúdo aprovado é escrito no lugar do marcador abaixo (ver TypstTemplateMaterializer). Pode
// editar o visual das linhas acima do marcador; não escreva aqui nenhum fato pessoal ou exemplo.
#let lines(text) = text.split("\n").map(line => grid(columns: (1.1em, 1fr), [•], [#line])).join()
#let compact = false // tailorly:compact

#set document(title: "Currículo") // tailorly:title
#set page(paper: "a4", margin: if compact { (x: 1.6cm, y: 1.3cm) } else { (x: 2cm, y: 1.7cm) })
#set text(font: ("Libertinus Serif", "New Computer Modern", "DejaVu Serif"), size: 10.5pt, lang: "pt") // tailorly:font-size
#set par(justify: false, leading: if compact { 0.42em } else { 0.5em }, spacing: if compact { 0.45em } else { 0.6em })
#set list(indent: 0.4em, body-indent: 0.5em)

#show heading.where(level: 2): it => block(above: if compact { 0.8em } else { 1.2em }, below: 0.5em, width: 100%)[
  #text(size: 1.05em, weight: "bold", smallcaps(it.body))
  #v(-0.4em)
  #line(length: 100%, stroke: 0.7pt)
]

// tailorly:content
