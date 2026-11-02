# Estrutura do frontend

Organização de `apps/web/src`, regras de onde cada tipo de código mora, aliases de import e rotas.

## Árvore

```text
src/
├── main.tsx                    # monta StrictMode + QueryClientProvider + RouterProvider
├── routeTree.gen.ts            # GERADO pelo plugin do TanStack Router — não editar
├── styles.css                  # tokens de design + @theme inline + estilos base
├── pages/                      # rotas por arquivo (ver "Organização por rota")
│   ├── __root.tsx              # layout raiz: TooltipProvider, HeadContent, Header, Outlet
│   └── _app/                   # prefixo de rota omitido na URL
│       ├── _home/              # "/"              — vaga alvo, etapas e últimos currículos
│       ├── base/               # "/base"          — auditoria e revisão dos fatos do Currículo Base
│       ├── cvs/                # "/cvs"           — histórico de currículos gerados
│       ├── generator/          # "/generator"     — vaga alvo, prévia do PDF e editor Typst
│       ├── due-diligence/      # "/due-diligence" — análise de empresas
│       └── profile/            # "/profile"       — perfil, padrões de geração, Second Brain e IA
├── components/                 # componentes compartilhados entre rotas
│   ├── ui/                     # design system vendorizado (Animate UI, badge, textura)
│   ├── fields/                 # campos de formulário reutilizáveis
│   ├── quick-review/           # diálogo de validação antes de gerar
│   ├── evidence/, generation/  # evidências complementares e andamento da geração
│   ├── ai-progress/            # rótulos de etapa da IA (bolinha animada)
│   ├── notifications/          # toasts e sino de notificações
│   ├── account/                # formulário de login e cadastro
│   └── header.tsx, account-access.tsx, ...
├── hooks/                      # hooks compartilhados (estado + efeitos, sem JSX)
├── queries/                    # integrações TanStack Query por recurso da API
├── stores/                     # stores Zustand + resetClientState
├── types/                      # tipos de domínio compartilhados
├── utils/                      # funções puras, sem React, agrupadas por domínio
│   ├── review/                 # revisão dos fatos: formatação, pendências, etapas, governança
│   ├── job/                    # vaga: URL, entrada, rascunho, casamento com os fatos
│   ├── extraction/             # polling e mensagens de falha da extração
│   ├── generation/             # regra de geração e nota de ATS
│   └── date-format.ts, notice-messages.ts, ai-preferences.ts
├── constants/                  # rótulos, ícones e opções estáticas compartilhadas
├── lib/                        # infraestrutura: cliente HTTP (lib/api), query-client, cn, contexto
└── assets/                     # logo, fontes e imagens

scripts/test-stores.mjs         # runner dos testes unitários (stores e utils)
tests/unit/                     # testes unitários de stores e utils
tests/e2e/                      # jornadas de interface com Playwright
```

## Princípios de organização

Cada tipo de código mora num lugar só, para que dê para achar e mudar sem ler o
resto:

| Camada                        | Contém                                                | Não contém                                                           |
| ----------------------------- | ----------------------------------------------------- | -------------------------------------------------------------------- |
| Componente (`.tsx`)           | JSX, estilos e a ligação entre hooks e props          | regras de negócio, mapeamento de dados da API, constantes de rótulos |
| `hooks/` ou `-hooks/`         | estado, efeitos e orquestração de mutations           | JSX                                                                  |
| `utils/` ou `-utils/`         | funções puras (testáveis sem React)                   | estado, `useX`, acesso a stores                                      |
| `types/` ou `-types/`         | tipos e interfaces de domínio                         | lógica                                                               |
| `constants/` ou `-constants/` | rótulos, ícones, opções e limites estáticos           | lógica de negócio                                                    |
| `queries/`                    | chamadas à API e invalidação de cache                 | regras de apresentação                                               |
| `stores/`                     | estado de interface (nunca fonte de verdade de fatos) | chamadas à API fora de ações explícitas                              |

Regras práticas:

1. **Um componente, uma responsabilidade.** Se um `.tsx` passa de ~150 linhas ou
   mistura formulário, mapeamento de dados e apresentação, extraia o hook ou o
   util e quebre a marcação em subcomponentes. Componentes só de uma tela ficam
   em subpasta da própria tela (ex.: `-components/job-analysis/`).
2. **Compartilhado sobe, específico desce.** Algo usado por uma rota só fica na
   pasta dela; ao ser usado por duas, vai para `components/`, `hooks/`, `utils/`
   ou `types/`.
3. **Comentários em português**, explicando o _porquê_ (regra de domínio,
   armadilha, decisão), não o que a linha faz.
4. **Aliases em vez de `../..`.** Para sair da pasta atual use o alias da
   pasta (`@stores/...`, `@utils/...`); import relativo fica só para vizinhos
   (`./x`) e para arquivos da mesma rota (`-components`, `-hooks`...). O runner
   dos testes unitários resolve os mesmos aliases do `tsconfig.app.json`, então
   tudo que ele alcança também pode usá-los.

## Organização por rota

O TanStack Router ignora entradas que começam com `-` em `pages/`, então cada
rota guarda o que é só dela em subpastas com esse prefixo:

```text
pages/_app/<rota>/
├── index.tsx        # a rota: só compõe blocos e liga hooks
├── -components/     # componentes (e subpastas de componentes) da tela
├── -steps/          # (base) formulários de cada etapa da revisão
├── -hooks/          # hooks da tela
├── -utils/          # funções puras da tela
├── -types/          # tipos da tela
└── -constants/      # rótulos e limites da tela
```

Exemplos reais: `base/-hooks/use-save-review-draft.ts` grava o rascunho na API;
`base/-utils/review-draft-hydration.ts` converte fatos da API em rascunho;
`generator/-hooks/use-typst-code-editor.ts` cuida do CodeMirror;
`generator/-components/resizable-split.tsx` é o divisor arrastável do modo dividido e
`generator/-components/pdf-live-viewer.tsx` desenha a prévia do PDF no lugar (pdf.js);
a lógica pura do editor (diagnósticos, cores, autocompletar, edições) fica em `generator/-utils/typst-*.ts`, com testes em `tests/unit/typst-editor.test.ts`;
`generator/-components/job-analysis/` divide o cartão de análise da vaga.

## Aliases

Definidos em **três lugares que precisam concordar**: `vite.config.ts` (alias de
runtime) e `tsconfig.json` + `tsconfig.app.json` (alias de tipo). O runner dos
testes unitários lê os de `tsconfig.app.json`. Ao adicionar um alias, atualize
os três.

| Alias           | Destino                       | Use para                                              |
| --------------- | ----------------------------- | ----------------------------------------------------- |
| `@components/*` | `src/components/*`            | componentes compartilhados                            |
| `@ui/*`         | `src/components/ui/*`         | componentes soltos do design system (badge, textura)  |
| `@animate/*`    | `src/components/ui/animate/*` | componentes Animate UI (botão, diálogo, tabs...)      |
| `@assets/*`     | `src/assets/*`                | imagens e fontes                                      |
| `@stores/*`     | `src/stores/*`                | stores Zustand                                        |
| `@queries/*`    | `src/queries/*`               | integrações TanStack Query                            |
| `@hooks/*`      | `src/hooks/*`                 | hooks compartilhados                                  |
| `@utils/*`      | `src/utils/*`                 | funções puras (`@utils/review/...`, `@utils/job/...`) |
| `@constants/*`  | `src/constants/*`             | rótulos, ícones e opções estáticas                    |
| `@@types/*`     | `src/types/*`                 | tipos de domínio                                      |
| `@api/*`        | `src/lib/api/*`               | cliente HTTP, erros, chaves de query e código gerado  |
| `@lib/*`        | `src/lib/*`                   | infraestrutura (`cn`, query client, contexto)         |
| `@app/*`        | `src/pages/_app/*`            | arquivos de uma rota usados fora dela                 |
| `@pages/*`      | `src/pages/*`                 | raro: o layout raiz                                   |
| `@/*`           | `src/*`                       | o que não tem alias próprio (`@/main`)                |

O alias mais específico vence: prefira `@api/client` a `@lib/api/client` e
`@utils/job/job-url` a `@/utils/job/job-url`.

> `@ui` e `@animate` apontam para pastas diferentes: use `@animate/base/tabs`
> para componentes Animate e `@ui/badge` para componentes soltos.

## Rotas

`src/routeTree.gen.ts` é **gerado**. Nunca edite. O prefixo de arquivo `_app`
não aparece na URL, e o `_home` também não. Descrição de cada tela em [Telas](screens.md).

| URL              | Arquivo                                  | Papel                                                            |
| ---------------- | ---------------------------------------- | ---------------------------------------------------------------- |
| `/`              | `src/pages/_app/_home/index.tsx`         | Envio do Currículo Base, vaga alvo e últimos currículos          |
| `/base`          | `src/pages/_app/base/index.tsx`          | Upload, andamento, texto bruto por página e revisão persistida   |
| `/generator`     | `src/pages/_app/generator/index.tsx`     | Vaga alvo, prévia do PDF, editor Typst, nota de ATS e evidências |
| `/cvs`           | `src/pages/_app/cvs/index.tsx`           | "Meus Currículos": histórico, download e exclusão                |
| `/due-diligence` | `src/pages/_app/due-diligence/index.tsx` | Relatórios de due diligence de empresas                          |
| `/profile`       | `src/pages/_app/profile/index.tsx`       | Perfil, Ground Truth, Second Brain e preferências de IA          |
