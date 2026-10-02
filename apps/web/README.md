# Tailorly Web — `apps/web`

Interface React do Tailorly: ingestão do Currículo Base, auditoria dos fatos em
seis dimensões, revisão rápida de pendências e preparação para a geração.

> **Estado atual:** fundação visual e de estado de interface. A home, a
> auditoria do Currículo Base (`/base`) e a revisão rápida estão implementadas
> como **prévias locais**: nada é enviado, não há extração, OCR, persistência,
> autenticação nem geração real. `/cvs` e `/generator` são placeholders.

> **Regra de leitura:** este README separa sempre o **fluxo alvo** (descrito no
> README do monorepo) da **implementação atual**. Uma funcionalidade marcada
> como *Planejada* aqui não existe no código.

---

## 1. Stack

| Aspecto | Implementação |
| --- | --- |
| Runtime | React 19, TypeScript 5.8, Vite 7 |
| Estilo | Tailwind CSS 4 (config CSS-first em `src/styles.css`) |
| Roteamento | TanStack Router 1.x, rotas por arquivo em `src/pages` |
| Dados remotos | TanStack Query 5; `QueryClient` único no contexto do router |
| Estado local | Zustand 5, seletores por propriedade |
| Primitivos de UI | Base UI (`@base-ui/react`) e Animate UI vendorizado |
| Ícones | `@react-zero-ui/icon-sprite` (Lucide + Tabler) |
| Movimento | `motion` (Motion for React) |
| Formatação/lint | Biome 2.5 (sem ESLint/Prettier) |
| Validação de esquema | `zod` 4 (dependência declarada; ainda sem uso no estado atual) |

### Comandos

| Comando | O que faz |
| --- | --- |
| `pnpm dev` | Sobe o Vite em `http://localhost:5173` com proxy de `/api` para `http://localhost:8080`. |
| `pnpm build` | Executa `prebuild` (`zero-icons`) e depois `vite build && tsc -b`. |
| `pnpm lint` | `biome lint` |
| `pnpm check` | `biome check` (formatação + lint) |
| `pnpm test:stores` | Suite de estado do cliente. Ver §11. |

---

## 2. Estrutura

```text
src/
├── main.tsx                    # monta StrictMode + QueryClientProvider + RouterProvider
├── routeTree.gen.ts            # GERADO pelo plugin do TanStack Router — não editar
├── styles.css                  # tokens de design + @theme inline + estilos base
├── pages/                      # rotas por arquivo
│   ├── __root.tsx              # layout raiz: TooltipProvider, HeadContent, Header, Outlet
│   └── _app/                   # prefixo de rota omitido na URL
│       ├── _home/index.tsx     # "/"        — hero, etapas, recentes, Second Brain
│       ├── base/               # "/base"    — auditoria do Currículo Base
│       │   ├── -components/    # 9 componentes da auditoria
│       │   └── -steps/         # 6 etapas, uma por dimensão
│       ├── cvs/index.tsx       # "/cvs"     — placeholder
│       └── generator/index.tsx # "/generator" — placeholder
├── components/
│   ├── header.tsx, page-placeholder.tsx
│   └── ui/                     # design system vendorizado
│       ├── badge.tsx, noise-texture.tsx
│       └── animate/            # Animate UI — ver §5
├── stores/                     # 7 stores Zustand + resetClientState
├── types/                      # review.ts, review-model.ts
├── utils/review-issues.ts      # regras de seleção e completude de pendências
├── data/review-preview.ts      # fixtures de demonstração
├── hooks/                      # use-auto-height, use-controlled-state,
│                               # use-data-state, use-is-in-view
├── lib/                        # query-client, utils (re-export de cn), get-strict-context
└── assets/tailorly-logo.svg

scripts/test-stores.mjs         # runner da suite de estado
tests/client-state.test.ts      # 8 testes de estado do cliente
```

### Aliases

Definidos em **três lugares que precisam concordar**: `vite.config.ts` (alias de
runtime) e `tsconfig.json` + `tsconfig.app.json` (alias de tipo). Ao adicionar um
alias, atualize os três.

| Alias | Destino |
| --- | --- |
| `@components/*` | `src/components/*` |
| `@ui/*` | `src/components/ui/*` |
| `@animate/*` | `src/components/ui/animate/*` |
| `@assets/*` | `src/assets/*` |
| `@stores/*` | `src/stores/*` |
| `@pages/*` | `src/pages/*` |
| `@lib/*` | `src/lib/*` |
| `@hooks/*` | `src/hooks/*` |
| `@app/*` | `src/pages/_app/*` |
| `@@types/*` | `src/types/*` |
| `@/*` | `src/*` |

> `@ui` e `@animate` são o mesmo prefixo em l'import: use `@animate/base/tabs`
> para componentes Animate e `@ui/badge` para componentes soltos.

### Rotas

`src/routeTree.gen.ts` é **gerado**. Nunca edite. O prefixo de arquivo `_app`
não aparece na URL, e o `_home` também não.

| URL | Arquivo | Estado |
| --- | --- | --- |
| `/` | `src/pages/_app/_home/index.tsx` | Funcional (prévia local) |
| `/base` | `src/pages/_app/base/index.tsx` | Funcional (sem extração) |
| `/cvs` | `src/pages/_app/cvs/index.tsx` | Placeholder ("Meus Currículos") |
| `/generator` | `src/pages/_app/generator/index.tsx` | Placeholder ("Gerador") |

---

## 3. Design system

### Tokens

`src/styles.css` tem três blocos: variáveis cruas em `:root` (nomes sem
prefixo), o mapeamento `@theme inline` (que expõe os tokens como utilities do
Tailwind) e os estilos base.

**Paleta "Warm Editorial Executive"** — três famílias com escala de 50 a 950:

| Família | Papel | Âncora |
| --- | --- | --- |
| `primary` | Rich Espresso | `--primary-900: var(--material-primary)`, `--primary-950: var(--primary-container)` |
| `secondary` | Warm Walnut | `--secondary-800: var(--material-secondary)` |
| `tertiary` | Golden Amber | `--tertiary-600: var(--material-tertiary)` |

Sobre elas vêm os papéis Material (`surface`, `surface-container-lowest` até
`-highest`, `on-surface*`, `outline*`, `*-container`, `*-fixed`, `error*`, e
`inverse-*`), os semânticos shadcn (`background`, `foreground`, `card`, `popover`,
`primary`, `muted`, `accent`, `destructive`, `border`, `input`, `ring`, `chart-1..5`,
`sidebar*`), mais `--shadow-1..3`, `--radius`, `--gutter` e `--canvas-margin`.

> Ao escolher um tom de superfície use a **escala de container**: `card` para
> superfícies elevadas (correspondendo a `--surface-container-lowest`),
> `bg-surface-container-low` para campos e blocos internos,
> `bg-surface-container` para chrome de fundo. Misturar `card` e
> `bg-white` gera inconsistência — `steps-section.tsx` ainda usa `bg-white` e é
> um ponto conhecido de inconsistência.

**Tipografia.** Inter Variable, exposta como scale semântica em `@theme inline`:

| Token | Tamanho / linha |
| --- | --- |
| `display-lg` | 3.5rem / 4rem, 700, `-0.025em` |
| `headline-lg` | 2rem / 2.5rem, 600 |
| `headline-lg-mobile` | 1.625rem / 2.125rem, 600 |
| `headline-md` | 1.5rem / 2rem, 600 |
| `headline-sm` | 1.25rem / 1.75rem, 600 |
| `title-md` | 1rem / 1.5rem, 600 |
| `body-lg` / `body-md` / `body-sm` | 1.125 / 0.9375 / 0.8125rem |
| `label-md` / `label-sm` | 0.875 / 0.75rem, com tracking |

**Raios e espaçamento.** `--radius-sm` a `--radius-2xl` derivam de `--radius`
(aritmética `calc`), mais `--radius-full`. Espaçamento inclui `--spacing-gutter`
e `--spacing-space-xs..xl`.

> **Atenção — configuração duplicada.** `tailwind.config.ts` existe e repete as
> cores, tamanhos de fonte e raios, mas aponta para variáveis que **não
> existem** (`--type-display-lg`, `--space-xs`, `--radius-3xl`, `--radius-4xl`
> — o CSS define `--text-*` e `--spacing-*`). Com Tailwind 4 a config é
> CSS-first, então o arquivo é praticamente inerte: as utilities resolvem pelo
> `@theme inline`. Trate `src/styles.css` como a fonte da verdade e não invista
> em `tailwind.config.ts` sem decidir primeiro se ele deve ser removido.

### Movimento reduzido

Duas abordagens complementares:

- `useReducedMotion()` de `motion/react` — usado em componentes para zerar
  durações, `initial`/`exit` e `whileTap`/`whileHover`.
- a variante `motion-reduce:` do Tailwind — usada em `transition-colors`,
  `transition-transform` e `animate-ping`.

Componentes animados que **precisam** respeitar as duas:
`review-stepper.tsx`, `review-workspace.tsx`, `review-draft-collection.tsx`,
`preferences-review-step.tsx`, `review-correction-input.tsx`,
`review-month-picker.tsx`, `base-cv-picker.tsx`, `hero-ingestion-card.tsx`,
`header.tsx`, `review-section.tsx`.

---

## 4. Camadas de componente

```
primitives/  →  Base UI + motion/react  (implementação)
   ↑
base/ buttons/  →  cva + tokens do projeto  (camada de uso)
   ↑
components/     →  header, page-placeholder, badge, noise-texture
```

### `ui/animate/primitives/` — implementação

| Componente | Base |
| --- | --- |
| `animate/slot` | Slot `asChild` sobre `motion` |
| `animate/tabs` | Tabs com highlight animado |
| `base/*` | `alert-dialog`, `checkbox`, `dialog`, `menu`, `progress`, `switch`, `tabs`, `toggle`, `toggle-group`, `tooltip` — primitivos Base UI com animação |
| `buttons/button` | `motion.button` com `hoverScale` (1.025) e `tapScale` (0.98), neutros sob movimento reduzido |
| `buttons/ripple` | Botão com ripple |
| `effects/auto-height` | Anima altura via `useAutoHeight` |
| `effects/highlight` | Highlight deslizante (`mode="parent"`, `controlledItems`) |
| `texts/counting-number` | Número animado |

### `ui/animate/base/` e `ui/animate/buttons/` — camada de uso

Reexportam os primitivos e aplicam os tokens do projeto via `cva`/`cn`. São
estes que as páginas importam.

- `buttons/button` — variantes `default`, `accent`, `destructive`, `outline`,
  `secondary`, `ghost`, `link`; tamanhos `default`, `sm`, `lg`, `icon`,
  `icon-sm`, `icon-lg`; prop extra `disableZoom` que fixa `hoverScale`/`tapScale`
  em 1 (use em botões dentro de listas densas).
- `base/*` — mesmos primitivos com defaults de layout do projeto.
- `buttons/copy`, `community/share-button` — auxiliares.

> Há duplicação aparente entre `base/tabs.tsx` e `primitives/base/tabs.tsx`: o
> primeiro **importa** o segundo e só injeta classes. O grafo de import é
> sempre `primitives → base`, nunca o inverso.

### Ícones

`@react-zero-ui/icon-sprite` (ícones Lucide e Tabler). O `prebuild` roda
`zero-icons`, que gera `public/icons.svg` **apenas com os ícones importados**; o
Vite o inclui no build. Em desenvolvimento a biblioteca renderiza os SVGs
diretamente. `public/icons.svg` está no `.gitignore` — é gerado, não versionado.

`components.json` declara `iconLibrary: "lucide"` para o gerador shadcn, mas
componentes adicionados por ele devem trocar os imports gerados para
`@react-zero-ui/icon-sprite`.

---

## 5. Domínio de revisão

### As seis dimensões

Definidas em `src/types/review-model.ts` como `reviewSteps`, e renderizadas na
ordem pelo seletor de etapas:

| # | `id` | Título na UI | Coleção (`DraftCollection`) |
| --- | --- | --- | --- |
| 1 | `identity` | Identidade & Contato | campos soltos (`DraftFields`) |
| 2 | `experience` | Experiência Profissional | `experience` |
| 3 | `projects` | Projetos & Métricas | `projects` |
| 4 | `education` | Formação & Certificados | `degrees`, `certifications` |
| 5 | `skills` | Competências & Idiomas | `languages`, `technologies` |
| 6 | `preferences` | Preferências | fora do rascunho: `usePresentationStore` |

Cada etapa renderiza dentro de `ReviewStepShell`, que exibe um número, título,
descrição, um selo ("Aguardando revisão" ou "Ajustes editoriais") e uma
**"Regra de integridade"** — texto que declara a restrição editorial daquela
dimensão. A etapa 3 (Projetos) recebe destaque visual na borda da regra.

### Campos por dimensão

Definidos como `const fields: ReviewField[]` dentro de cada arquivo em `-steps/`.
`ReviewField` = `{ key, label, type?, hint?, wide? }`, com `type` em
`text | email | tel | url | month | textarea`.

| Etapa | Chaves |
| --- | --- |
| Identidade | `name`, `location`, `email`, `phone`, `linkedin`, `github` |
| Experiência | `company`, `role`, `employment`, `status`, `start`, `end`, `description` |
| Projetos | `title`, `context`, `statement`, `metric`, `source` |
| Formação | `degree`, `institution`, `status`, `start`, `end` |
| Certificações | `name`, `issuer`, `credential`, `issued`, `expires` |
| Idiomas | `language`, `proficiency` |
| Tecnologias | `name`, `context` |

Registros podem ser **adicionados, editados, recolhidos e removidos**.
`ReviewDraftCollection` cria o `id` do registro com `crypto.randomUUID()` e
rotula cada um com "Rascunho local • não validado". Ao remover, o foco volta
para o botão de adicionar.

Botões desabilitados que existem como especificação de interface: **"Validar
todos os fatos"** (etapa 3), **"Ver trecho original no OCR"** (quando
`evidence`), o switch **"Bounding Boxes de IA"**, "Salvar Rascunho da Auditoria"
e "Aprovar Ground Truth" — todos com `title` explicando que dependem da
integração com o servidor.

### Modelo de pendência (`src/types/review.ts`)

```ts
ReviewIssue = {
  id: string;
  category: ReviewCategory;      // 7 valores, ver abaixo
  type: "metric" | "temporal";
  question: string;
  evidence?: string;             // trecho de origem
  motivation: string;            // por que a vaga exige esta confirmação
  choices: readonly ReviewChoice[];
}
```

`ReviewCategory` tem **sete** valores — `identity`, `employment`, `projects`,
`education`, `skills`, `languages`, `presentation` — mais um que as seis
dimensões, porque `languages` é categoria de pendência separada de `skills`.

`ReviewChoice` é uma união discriminada por `result`:

| `result` | Forma | Efeito |
| --- | --- | --- |
| `"confirmed"` | `{ value, label, result }` | completa sem texto extra |
| `"omitted"` | `{ value, label, result }` | completa; exclui o dado |
| `"corrected"` | `{ value, label, result, correction }` | exige `CorrectionField` |

`CorrectionField` = `{ label, type, placeholder?, min?, max?, originalValue?,
options? }`, com `type` em `text | email | month | number | select`.

### Regras em `src/utils/review-issues.ts`

**`selectQuickReviewIssues`** — recorta a lista para a revisão rápida: no máximo
**3 pendências**, e no máximo **2 categorias distintas** representadas (ao
atingir 2 categorias, pula as demais). Não é aleatório nem por prioridade; é
corte por ordem de chegada com teto de diversidade.

**`isReviewIssueComplete(issue, answer)`** — decide se uma pendência está
respondida:

1. Sem resposta, ou com `choice` inexistente → incompleta.
2. Escolha sem `correction` → completa.
3. Com `correction`: o texto é aparado e rejeitado se vazio **ou igual a
   `originalValue`** (é preciso mudar algo de fato).
4. Validação por tipo:

| Tipo | Regra |
| --- | --- |
| `month` | `^\d{4}-(0[1-9]\|1[0-2])$` e entre `min` (padrão `"0000-01"`) e `max` (padrão: mês corrente) |
| `number` | finito, ≥ `min` (padrão 0), ≤ `max` se definido, e ≠ `originalValue` |
| `email` | `/^[^\s@]+@[^\s@]+\.[^\s@]+$/` |
| `select` | deve ser um valor de `options` |
| `text` | qualquer texto não vazio |

`reviewCategories` é o mapa `ReviewCategory` → rótulo em português usado nos
cabeçalhos da revisão rápida.

---

## 6. Estado compartilhado da interface

Sete stores Zustand, sempre consumidas por seletor de propriedade. O estado
sobrevive à navegação entre rotas na mesma aba, **sem persistência**: recarregar
ou fechar a aba descarta tudo.

Arquivos e rascunhos **não são** fatos aprovados, cache de OCR, histórico
persistente nem fonte de cotas.

| Store | Estado | Responsabilidade |
| --- | --- | --- |
| `useBaseCvStore` | `file`, `selectionId`, `error` | Arquivo compartilhado entre home e `/base`, identificação local da seleção, validação de formato/vazio, erro |
| `useReviewDraftStore` | `sourceId`, `draft`, `activeStep`, `expandedRecords` | Rascunhos das seis dimensões, etapa ativa, cartões expandidos — ligados a `sourceId` |
| `useQuickReviewStore` | `open`, `contextKey`, `issues`, `preview`, `answers`, `submittedPreview` | Abertura, pendências, respostas e snapshot da prévia, vinculados a `contextKey` |
| `useJobDraftStore` | `mode`, `jobUrl`, `jobText`, `revision`, `savedAt` | URL/texto da vaga, modo de entrada, revisão e horário da última ação de salvar (em memória) |
| `usePresentationStore` | `preferences` | Formato do nome, idioma, links de saída, filtro de senioridade. Preferências **editoriais**, independentes dos fatos |
| `useDocumentViewerStore` | `zoom` | Zoom compartilhado; `"fit"` ou número limitado a 50–200 |
| `useWorkspaceStore` | `activeView` | Modo `editor` / `preview` do workspace |

### Seleção de arquivo

`selectFile(file, format = "document")` devolve `boolean`:

- **Formato.** `format: "pdf"` aceita só `/\.pdf$/i`; o padrão aceita
  `/\.(pdf|doc|docx)$/i`. O seletor de `/base` usa `"pdf"`; o da home usa o
  padrão.
- **Arquivo vazio** (`size === 0`) é rejeitado.
- **Erro de seleção não apaga o arquivo anterior** — apenas grava `error`.
- **Mesma referência** (`file === get().file`) apenas limpa o erro e retorna
  `true`, preservando rascunhos. **Isto não é deduplicação por hash.**
- **Arquivo diferente** gera um `selectionId` novo com `crypto.randomUUID()` e
  **limpa** rascunhos, respostas rápidas, cartões expandidos e zoom. O
  comentário no código é explícito: *"A local selection ID is not a Ground Truth
  version or a content hash."*
- `selectFile(null)` remove: zera `file` e `selectionId` e reaplica as limpezas.

DOC/DOCX são aceitos pela home e compartilhados, mas **somente PDFs têm
visualização local**.

### Guardas de contexto obsoleto

Dois mecanismos independentes impedem callbacks atrasados de sujar o rascunho atual:

- **`review-draft-store`**: `setIdentity`, `setCollection` e `setExpandedRecord`
  recebem `sourceId` e devolvem o estado **inalterado** quando
  `sourceId !== state.sourceId`. Em `setExpandedRecord` o id do registro só é
  aplicado se a fonte casar.
- **`quick-review-store`**: `setAnswer` e `savePreview` recebem `contextKey`;
  `setAnswer` também valida que a pendência existe em `issues` **e** que o
  `answer.choice` está entre as escolhas dela. `configure` zera respostas
  quando a chave muda, mas **preserva** quando a chave é a mesma — por isso as
  respostas sobrevivem a remontagens.

`ValidateDialog` monta a `contextKey` como
`JSON.stringify([selectionId, jobRevision, preview, allIssues, requiresFullReview])`
e, em cada render, só usa as respostas armazenadas se
`storedContext === contextKey`; caso contrário trata como `{}`.

### Vaga × Currículo Base

- Mudar a vaga (`setMode`, `setJobUrl`, `setJobText`) **invalida as respostas
  rápidas** (`useQuickReviewStore.reset()`) e incrementa `revision`. Valores
  idênticos são no-op (comparação campo a campo) e não invalidam nada.
- `useJobDraftStore.reset()` também incrementa `revision`, então a
  `contextKey` antiga nunca volta a casar.
- Trocar o Currículo Base **não** altera a vaga nem as preferências
  editoriais — coberto por teste.

### Preferências de apresentação

`PresentationPreferences` = `{ nameFormat: "short" | "full", language: "pt-BR" |
"en-US" | "es-ES", links: { linkedin, github, whatsapp }, hideOlderRoles }`.
Padrões em `initialPreferences`: nome curto, `pt-BR`, LinkedIn e GitHub ativos,
WhatsApp inativo, ocultar cargos antigos **ativo**.

`usePresentationStore` clona `links` na escrita e usa uma função `defaults()`
que faz spread profundo de `links`, para que duas stores nunca compartilhem
referência.

### `resetClientState()`

Em `src/stores/reset-client-state.ts`. Reseta, nesta ordem: `useBaseCvStore`
(que em cascata limpa rascunhos, revisão rápida e visualizador),
`useJobDraftStore`, `usePresentationStore`, `useWorkspaceStore`. Depois remove
duas chaves **legadas** de `sessionStorage` —
`tailorly:job-draft` e `tailorly:review-preview` — deixadas pela antiga
implementação com drafts por componente. O `try/catch` existe porque o navegador
pode negar acesso ao storage; a memória é limpa de todo modo.

Deve ser chamado pelos futuros fluxos de saída, troca de conta e expiração de
sessão. **Autenticação e TTL no servidor ainda não existem**: a função **não**
representa exclusão de arquivos ou artefatos no servidor.

### Estado que é deliberadamente local

Foco, hover, arraste, calendário aberto e medidas de animação ficam em
`useState`/`useRef` do componente, não em store.

A **URL de objeto da prévia** é recurso do visualizador: criada quando
`MasterDocumentPanel` monta, revogada na troca, remoção ou desmontagem, enquanto
o `File` permanece na store. O painel compara `preview?.file === file` para nunca
exibir o documento anterior enquanto a URL substituta é criada.

**Textos, definições de campos e fixtures são dados estáticos**, não stores
mutáveis. `src/data/review-preview.ts` guarda duas pendências de demonstração
com o comentário *"Explicit demonstration data, never extracted from the selected
document"*.

---

## 7. Fluxo da revisão rápida

1. `HeroIngestionCard` (`/`) mostra o seletor de Currículo Base e as abas
   **URL** / **Texto** da vaga. O botão "Gerar currículo sob medida" só habilita
   com vaga **e** Currículo Base presentes; ao clicar chama `saveDraft()` e abre
   a revisão.
2. Sem `issues` prop, `ValidateDialog` usa `previewReviewIssues` — é o modo
   demonstração.
3. `configure(contextKey, allIssues, preview)` roda em `useEffect`.
4. `selectQuickReviewIssues` recorta para até 3 pendências / 2 categorias.
5. Se sobraram pendências (`needsFullReview`), o diálogo **não** oferece
   "Continuar para prévia": manda para `/base`. O botão só aparece quando
   `quickIssues.length === allIssues.length` e `!requiresFullReview`.
6. Cada pendência é um `ReviewSection` com as escolhas; escolher `corrected`
   revela um `ReviewCorrectionInput` com o controle adequado ao `type` (input
   numérico com stepper, `Select` do Base UI, `ReviewMonthPicker` em popover, ou
   input simples). Valor inválido mostra "Informe um valor válido e diferente do
   original."
7. "Continuar para prévia" só habilita com todas as pendências completas;
   então salva, fecha e navega para `/generator`.

Rótulos do modo demonstração no diálogo: "Prévia da revisão de currículo",
`"N exemplos de revisão"`, e *"Demonstração com dados fictícios, não extraídos
do seu arquivo. As respostas ficam apenas como rascunho de interface."*

---

## 8. Home (`/`)

| Seção | Conteúdo |
| --- | --- |
| `HeroSection` | Badge "Arquitetura Ground Truth", título, e a promessa "Sem inventar fatos". |
| `StepsSection` | Metodologia em 4 etapas: Ground Truth → due diligence da vaga → análise/ATS → síntese sob medida. |
| `RecentCVSection` | "Últimos Currículos Gerados" com três entradas **de demonstração** codificadas em `const recentCVs` e o link "Ver todos (18)". |
| `SecondBrainSection` | Integração Notion/Obsidian apresentada como **recurso extra e opcional**; o botão secundário é "Continuar apenas com Currículo Base". |

`BaseCvPicker` aceita **um único** arquivo (`.pdf`, `.doc`, `.docx`) por input
ou arraste, com contador de profundidade para o estado de arraste. O arquivo
selecionado mostra o selo "Prévia local • ainda não processado".

> **Desvio de invariante conhecido.** `RecentCVSection` apresenta três
> currículos fictícios — com "96% ATS Match", empresa e data — como se fossem
> documentos da pessoa, e um contador "(18)". Isso contraria a regra do README
> do monorepo e do `AGENTS.md`: *a interface de "Últimos Currículos Gerados" deve
> mostrar estado vazio ou de prévia para visitantes sem histórico, nunca
> exemplos como se fossem documentos reais*. A seção precisa virar estado vazio
> ou de prévia antes de qualquer lançamento. O mesmo vale para os atalhos
> "Testar com 1 clique" (`quickTests`), que preenchem a vaga com texto de
> exemplo.

---

## 9. Currículo Base (`/base`) — auditoria

Interface derivada dos nós do Figma Tailorly, usando os tokens do projeto,
ícones do sprite e Button/AutoHeight do Animate UI.

### Cabeçalho

Trilha de navegação (`Início / Currículo Base / Auditoria & Curadoria do
Ground Truth`), selo "Aguardando Currículo Base", título "Auditoria & Curadoria
dos Fatos do Currículo Base", input de PDF e três `GovernanceCard`:
**Arquivo mestre** (nome e tamanho, "Prévia local, sem envio"), **Índice de
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
- Sem arquivo: ação de seleção e zoom desabilitado. Com DOC/DOCX: mensagem de
  que a visualização local é só para PDF.
- Rodapé "Cofre Factual Fechado": *"Nenhum fato, empresa, número ou métrica
  além deste documento será inventado ou inferido pela inteligência."*

### Seletor de etapas

`ReviewStepper` é uma `nav` com `<ol>` responsivo por **container queries**
(`@container`): 1 coluna → 2 em `@xs` → 3 em `@xl` → 6 em `@[68rem]`. O
`Highlight` (`pointer-events-none`) desloca o fundo da etapa ativa. Navegação
por clique no seletor ou pelos botões **Anterior** / **Próxima**, com
`aria-live` anunciando "Etapa N de 6". `ReviewCompletionCard` aparece só na
etapa 6.

---

## 10. Qualidade e tooling

### TypeScript

`tsconfig.json` é solução com referências a `tsconfig.app.json` e
`tsconfig.node.json`. Ambos com `strict: true`, `noEmit: true`,
`moduleResolution: "Bundler"`, `verbatimModuleSyntax: true`,
`moduleDetection: "force"`. App: `target ES2022`, `jsx: "react-jsx"`,
`useDefineForClassFields`, `skipLibCheck`, `allowJs: false`. Node (para
`vite.config.ts`): `target ES2023`.

`build` roda `tsc -b` **após** o `vite build`, então erros de tipo quebram o
build mesmo quando o bundle sai.

### Biome

Sem ESLint e sem Prettier. `"root": false` + `"extends": "//"` herda a config da
raiz do monorepo. Presets `recommended` de lint e assist; `organizeImports` em
`assist.actions.source`. Formatter: 2 espaços, `lineWidth` 80, aspas duplas.
Ignora `dist`, `src/routeTree.gen.ts` e `src/assets/header/*.svg`. O parser CSS
entende diretivas Tailwind.

Ignorar `src/routeTree.gen.ts` é obrigatório: o arquivo gerado usa
`@ts-nocheck` e `as any`.

### Hooks

| Hook | Assinatura / propósito |
| --- | --- |
| `useAutoHeight<T>(deps, options)` | `{ ref, height }`. Mede com `ResizeObserver`, opcionalmente incluindo padding/borda do pai quando `box-sizing: border-box`, e compensa `devicePixelRatio` para evitar altura fracionária. `includeParentBox` padrão `true`. |
| `useControlledState<T, Rest>(props)` | `[T, setter]` — estado controlado ou não, chamando `onChange` a partir do setter interno. |
| `useDataState(key, ref, onChange)` | Lê `data-{key}` do elemento como `useSyncExternalStore` via `MutationObserver`; normaliza `""`/`"true"`/`"false"` para `boolean`. |
| `useIsInView<T>(ref, { inView, inViewOnce, inViewMargin })` | `{ ref, isInView }` sobre `useInView` do Motion. |

`lib/get-strict-context.tsx` expõe um par `[Provider, useContext]` que **lança**
se o contexto for `undefined` — em vez de retornar `undefined` silencioso.
`lib/utils.ts` só reexporta `cn` do pacote `cn` (import direto de `"cn"` também
funciona, como em `review-section.tsx`).

---

## 11. Testes

```bash
pnpm test:stores   # node scripts/test-stores.mjs
```

`scripts/test-stores.mjs` **não adiciona dependências**. Ele:

1. cria um diretório temporário e escreve um `package.json` com
   `{ "type": "commonjs" }`;
2. faz `symlink` de `node_modules` para lá;
3. invoca o `tsc` **já instalado** (`node_modules/typescript/bin/tsc`) com
   `--module commonjs --target ES2022 --strict --skipLibCheck`, compilando
   `tests/client-state.test.ts` para fora do projeto;
4. executa o `.js` resultante com `node`;
5. propaga o `exit code` e apaga o temporário no `finally`.

Ou seja: usa `node:test` e `node:assert/strict` nativos e transpila via o
TypeScript do próprio projeto. Não há runner de teste em `devDependencies`.

`tests/client-state.test.ts` tem **8 testes**, com `beforeEach(resetClientState)`:

| # | Teste | O que fixa |
| --- | --- | --- |
| 1 | Seleção compartilhada preserva rascunhos, etapa e zoom | Consumidores diferentes leem o mesmo estado; reselecionar a **mesma** referência não limpa |
| 2 | Trocar a fonte reseta dados dependentes e rejeita atualizações obsoletas | Drafts, expandidos, etapa, respostas e zoom voltam ao padrão; `setIdentity` com id antigo é **ignorado**; vaga e preferências **preservadas** |
| 3 | Seleções inválidas e vazias preservam arquivo e rascunho | Arquivo vazio e DOCX no modo `pdf` retornam `false`, não apagam o arquivo, gravam `error` |
| 4 | Remover arquivo limpa rascunhos, revisão rápida e visualizador | `selectionId` e `file` viram `null`; `open` fecha |
| 5 | Mudança de vaga invalida revisão rápida sem apagar o draft do Currículo Base | `revision` incrementa, respostas zeram, identidade preservada |
| 6 | Revisão rápida mantém respostas ao remontar e rejeita issues antigas/desconhecidas | Reconfigurar com a **mesma** chave preserva; trocar de chave zera; `setAnswer` com chave ou id inválido é ignorado |
| 7 | Reset de sessão limpa todos os contextos e restaura padrões | Preferências voltam a `pt-BR` com LinkedIn ativo; workspace volta a `editor` |
| 8 | Visualizador impõe zoom suportado e ignora valores numéricos inválidos | 500→200, 1→50, `NaN`→mantém, `"fit"`→`"fit"` |

**Toda mudança nas stores ou em `reset-client-state.ts` precisa vir acompanhada
de um teste aqui.** É a única suíte do frontend.

---

## 12. O que **não** existe (planejado)

Nada abaixo está implementado no frontend. Tratar como especificação, não como
comportamento:

- envio de arquivo, OCR, extração de texto e extração de fatos;
- pendências geradas por extração real (as atuais são fixture de demonstração);
- evidência com origem no documento, consulta ao OCR, coordenadas de bounding box;
- estados de revisão `pendente` / `confirmado` / `corrigido` / `omitido`
  persistidos — hoje `result` existe só no tipo da escolha, não há ciclo de vida;
- versões candidatas, ativação atômica do Ground Truth, snapshot aprovado;
- hash de conteúdo e deduplicação por reenvio idêntico;
- salvamento remoto, aprovação, download, geração de PDF/Markdown;
- autenticação, sessão anônima, TTL, cotas no servidor;
- integração com o vault (Second Brain), Notion, Obsidian;
- persistência em `localStorage`/`sessionStorage` (só restam as duas chaves
  legadas, removidas em `resetClientState`);
- `POST /api/cv/generate` e qualquer outro endpoint de domínio.

Dados remotos devem entrar pela integração de **TanStack Query** já montada em
`src/lib/query-client.ts` e injetada no contexto do router. Estas stores **não**
promovem rascunhos a Ground Truth nem habilitam geração real.

Ao implementar, mantenha este README e o `AGENTS.md` sincronizados com o
comportamento efetivo e marque claramente o que segue planejado.