# Telas

O que cada rota faz hoje. A estrutura de arquivos está em [Estrutura](structure.md#rotas).

## Fluxo da revisão rápida

1. `HeroIngestionCard` (`/`) mostra o seletor de Currículo Base e as abas
   **URL** / **Texto** da vaga. O botão "Gerar currículo sob medida" só habilita
   com vaga **e** Currículo Base presentes; ao clicar chama `saveDraft()` e abre
   a revisão.
2. `ValidateDialog` carrega pendências reais da versão candidata via
   `/v1/review-issues/version/{baseVersionId}`. Sem versão autenticada, não mostra
   exemplos pessoais nem permite salvar respostas.
3. `configure(contextKey, allIssues, preview)` roda em `useEffect`.
4. Quando os dados vêm da API, o diálogo inclui todas as pendências reais da
   candidata; o limite de 3 pendências / 2 categorias só vale para issues de
   prévia fornecidas explicitamente.
5. Cada pendência persistida é respondida na API antes de continuar.
6. Cada pendência é um `ReviewSection` com as escolhas; escolher `corrected`
   revela um `ReviewCorrectionInput` com o controle adequado ao `type` (input
   numérico com stepper, `Select` do Base UI, `ReviewMonthPicker` em popover, ou
   input simples). Valor inválido mostra "Informe um valor válido e diferente do
   original."
7. "Continuar para prévia" só habilita com todas as pendências completas;
   então grava cada resposta na API, fecha e navega para `/generator`.

Sem versão autenticada, o diálogo informa que não há dados reais para revisar e
mantém a continuação desabilitada; não exibe fixtures como fatos da pessoa.

## Home (`/`)

| Seção                | Conteúdo                                                                                                                                                                                               |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `HeroSection`        | Badge "Arquitetura Ground Truth", título e a promessa "Sem inventar fatos".                                                                                                                            |
| `HeroIngestionCard`  | Seletor do Currículo Base, entrada da vaga (link ou texto, com validação imediata do link) e o botão de gerar. Os atalhos "Testar com 1 clique" só preenchem o texto da vaga com um título de exemplo. |
| `StepsSection`       | Metodologia em 4 etapas: Ground Truth → due diligence da vaga → análise/ATS → síntese sob medida.                                                                                                      |
| `RecentCVSection`    | "Últimos Currículos Gerados": o histórico real da conta ou sessão (`CvHistoryList`); sem histórico mostra um estado vazio, nunca exemplos como se fossem documentos da pessoa.                         |
| `SecondBrainSection` | Integração com Obsidian apresentada como **recurso extra e opcional**; o botão principal leva às conexões do perfil e o secundário é "Continuar apenas com Currículo Base".                            |

`BaseCvPicker` aceita **um único** PDF por input ou arraste, com contador de profundidade para o
estado de arraste. Mostra separadamente a seleção local e o arquivo atual; o selo acompanha o
resultado do upload.

## Currículo Base (`/base`) — auditoria

Interface derivada dos nós do Figma Tailorly, usando os tokens do projeto,
ícones do sprite e Button/AutoHeight do Animate UI.

### Cabeçalho

Trilha de navegação (`Início / Currículo Base / Auditoria & Curadoria do
Ground Truth`), selo com o estado do envio, título "Auditoria & Curadoria
dos Fatos do Currículo Base", input de PDF e três `GovernanceCard`:
**Arquivo mestre** (arquivo local ou nome do PDF enviado, com estado do upload), **Índice de
confiabilidade** ("Ainda não avaliado", barra vazia) e **Protocolo de
integridade** ("Aguardando revisão", "Nenhum fato confirmado"). Um
`aria-live="polite"` em `sr-only` anuncia a seleção.

### Painel Documento Mestre & OCR

Fica **fora** do seletor de etapas e permanece montado ao alternar a etapa.

- Zoom: inicia em `"fit"` (`#view=Fit` no fragmento do `iframe`), inclusive ao
  substituir o arquivo. O botão central restaura Fit; os laterais usam passo de
  25 e são clampados em 50–200 pela store. Saindo de Fit, o botão **menos**
  seleciona 50 e o **mais** seleciona 100. `aria-pressed` marca o estado Fit.
- O suporte a esses parâmetros depende do visualizador nativo do navegador.
- `key={`${url}-${zoom}`}` no `iframe` força remontagem ao mudar zoom.
- O switch **"Bounding Boxes de IA"** fica `checked={false} disabled`, pois não
  existem coordenadas de evidência.
- Link "Abrir PDF em outra aba" com `rel="noopener noreferrer"`.
- Sem arquivo: ação de seleção e zoom desabilitado.
- Rodapé "Cofre Factual Fechado": _"Nenhum fato, empresa, número ou métrica
  além deste documento será inventado ou inferido pela inteligência."_

### Seletor de etapas

`ReviewStepper` é uma `nav` com `<ol>` responsivo por **container queries**
(`@container`): 1 coluna → 2 em `@xs` → 3 em `@xl` → 6 em `@[68rem]`. O
`Highlight` (`pointer-events-none`) desloca o fundo da etapa ativa. Navegação
por clique no seletor ou pelos botões **Anterior** / **Próxima**, com
`aria-live` anunciando "Etapa N de 6". `ReviewCompletionCard` aparece só na
etapa 6.

---

## Gerador (`/generator`)

Vaga alvo (link ou texto), análise da vaga, seleção de layout, diretrizes e "otimizar para ATS",
card de ATS (nota por regras e leitura da IA), resumo das evidências complementares e as abas de
prévia: PDF ao vivo e editor Typst com divisor arrastável. Regras e endpoints em
[Geração de currículos](../../../docs/product/cv-generation.md) e
[Enriquecimento](../../../docs/product/enrichment.md). A lógica pura do editor fica em
`generator/-utils/typst-*.ts`, com testes em `tests/unit/typst-editor.test.ts`.

## Meus Currículos (`/cvs`)

Histórico de gerações com download (PDF ou Markdown) e exclusão individual.

## Due diligence (`/due-diligence`)

Formulário, histórico e relatório com os onze fatores, fontes, trechos e incerteza. Veja
[Due diligence](../../../docs/product/due-diligence.md).

## Perfil (`/profile`)

Abas: **Visão Geral & Perfil**, **Currículo Base (Ground Truth)**, **Conexões & Second Brain** e
**Preferências de IA & Provedores de LLM**, além dos padrões de geração (layout, diretrizes, ATS).
Visitantes não salvam preferências na conta.
