# Tooling

## TypeScript

`tsconfig.json` é solução (com os aliases de caminho) com referências a `tsconfig.app.json` e
`tsconfig.node.json`. Ambos com `strict: true`, `noEmit: true`,
`moduleResolution: "Bundler"`, `verbatimModuleSyntax: true`,
`moduleDetection: "force"`. App: `target ES2022`, `jsx: "react-jsx"`,
`useDefineForClassFields`, `skipLibCheck`, `allowJs: false`. Node (para
`vite.config.ts`): `target ES2023`.

`build` roda `tsc -b` **após** o `vite build`, então erros de tipo quebram o
build mesmo quando o bundle sai.

## Biome

Sem ESLint e sem Prettier. `"root": false` + `"extends": "//"` herda a config da
raiz do monorepo. Presets `recommended` de lint e assist; `organizeImports` em
`assist.actions.source`. Formatter: 2 espaços, `lineWidth` 80, aspas duplas.
Ignora `dist`, `src/routeTree.gen.ts` e `src/assets/header/*.svg`. O parser CSS
entende diretivas Tailwind.

Ignorar `src/routeTree.gen.ts` é obrigatório: o arquivo gerado usa
`@ts-nocheck` e `as any`.

## Hooks e utilitários compartilhados

| Hook                                                        | Assinatura / propósito                                                                                                                                                                                                      |
| ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `useAutoHeight<T>(deps, options)`                           | `{ ref, height }`. Mede com `ResizeObserver`, opcionalmente incluindo padding/borda do pai quando `box-sizing: border-box`, e compensa `devicePixelRatio` para evitar altura fracionária. `includeParentBox` padrão `true`. |
| `useControlledState<T, Rest>(props)`                        | `[T, setter]` — estado controlado ou não, chamando `onChange` a partir do setter interno.                                                                                                                                   |
| `useDataState(key, ref, onChange)`                          | Lê `data-{key}` do elemento como `useSyncExternalStore` via `MutationObserver`; normaliza `""`/`"true"`/`"false"` para `boolean`.                                                                                           |
| `useIsInView<T>(ref, { inView, inViewOnce, inViewMargin })` | `{ ref, isInView }` sobre `useInView` do Motion.                                                                                                                                                                            |

`lib/get-strict-context.tsx` expõe um par `[Provider, useContext]` que **lança**
se o contexto for `undefined` — em vez de retornar `undefined` silencioso.
`lib/utils.ts` só reexporta `cn` do pacote `cn` (import direto de `"cn"` também
funciona, como em `review-section.tsx`).

---

## Comandos

| Comando             | O que faz                                                                               |
| ------------------- | --------------------------------------------------------------------------------------- |
| `pnpm dev`          | Sobe o Vite em `http://localhost:5173` com proxy de `/v1` para `http://localhost:8080`. |
| `pnpm build`        | Executa `prebuild` (`zero-icons`) e depois `vite build && tsc -b`.                      |
| `pnpm lint`         | `biome lint`                                                                            |
| `pnpm check`        | `biome check` (formatação + lint)                                                       |
| `pnpm test`         | Testes unitários e e2e. Ver [Testes](testing.md).                                       |
| `pnpm api:generate` | Gera os tipos e as funções `fetch` com Orval a partir do snapshot versionado.           |
| `pnpm api:sync`     | Atualiza o snapshot pela API local e então executa a geração.                           |

## API: cliente gerado e produção

Os contratos e as funções HTTP são gerados pelo Orval em `src/lib/api/generated` (não edite). O
arquivo `src/lib/api/client.ts` é o mutator comum: adiciona `Accept-Language`, aceita `accessToken`,
envia o Bearer e normaliza erros como `ApiClientError`. Passe o token nas opções da função gerada, por
exemplo `currentAccountCurrent({ accessToken })`; elas podem ser usadas direto em `queryFn` e
`mutationFn`, sem impor chaves, cache ou política de retry. As consultas ficam em `src/queries`.

No desenvolvimento as chamadas usam caminhos relativos (`/v1/...`) e o proxy do Vite. No build de
produção, `VITE_API_URL` (ex.: `https://tailorly-api.onrender.com`) vira a origem de todas as chamadas
(`apiUrl` em `src/lib/api/client.ts`, via `define` no `vite.config.ts`). Na Vercel ela é uma variável
do projeto — mudar o valor exige um redeploy — e o `vercel.json` cuida do rewrite da SPA. Ver
[Deploy](../../../docs/deploy.md). O fluxo de regeneração do contrato está em
[Desenvolvimento local](../../../docs/development.md#contrato-openapi).

## Stack

| Aspecto          | Implementação                                                                   |
| ---------------- | ------------------------------------------------------------------------------- |
| Runtime          | React 19, TypeScript 5.8, Vite 7                                                |
| Estilo           | Tailwind CSS 4 (config CSS-first em `src/styles.css`)                           |
| Roteamento       | TanStack Router 1.x, rotas por arquivo em `src/pages`                           |
| Dados remotos    | TanStack Query 5; `QueryClient` único no contexto do router                     |
| Estado local     | Zustand 5, leituras por seletor ou por desestruturação da store                 |
| Primitivos de UI | Base UI (`@base-ui/react`) e Animate UI vendorizado                             |
| Ícones           | `@react-zero-ui/icon-sprite` (Lucide + Tabler)                                  |
| Movimento        | `motion` (Motion for React)                                                     |
| Editor Typst     | CodeMirror 6 com `codemirror-lang-typst` (parser Lezer, autocomplete e linting) |
| Prévia do PDF    | `pdfjs-dist` / `react-pdf`                                                      |
| Formatação/lint  | Biome 2.5 (sem ESLint/Prettier)                                                 |
