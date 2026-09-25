# Tailorly

Monorepo para geração de currículos personalizados a partir de uma vaga.

## Estrutura

- `apps/web`: interface React + Vite.
- `services/api`: API Spring Boot.

## Pré-requisitos

- Node.js 22+
- Java 21+

## Desenvolvimento

```bash
corepack enable
pnpm install
pnpm dev
```

O frontend abre em `http://localhost:5173` e a API em `http://localhost:8080`.

Para executar somente uma parte:

```bash
pnpm --filter @tailorly/web dev
pnpm --filter @tailorly/api dev
```

## Rotas do frontend

O TanStack Router gera as rotas a partir de `apps/web/src/pages` durante o desenvolvimento e o build. Como no Pages Router do Next, `pages/index.tsx` corresponde a `/` e `pages/curriculos/index.tsx` a `/curriculos`. Para parâmetros dinâmicos, o TanStack usa `$id.tsx` em vez de `[id].tsx`. O arquivo `pages/__root.tsx` define o layout raiz exigido pelo TanStack. A árvore gerada em `apps/web/src/routeTree.gen.ts` é parte do código do app e não deve ser editada manualmente.

## Dados e estado no frontend

O `QueryClientProvider` está configurado em `apps/web/src/main.tsx`, com o cliente compartilhado também no contexto das rotas para futuros loaders. Componentes de rota podem usar `useSuspenseQuery` dentro da boundary de `Suspense`; o fallback atual é vazio até a interface de carregamento ser criada. O estado local da interface começa em `apps/web/src/stores/workspace-store.ts` com Zustand.

## Próximas etapas

O domínio de currículos ainda será implementado. O primeiro fluxo será a ingestão de uma descrição ou URL de vaga, seguida de parsing, extração por LLM, template Typst, PDF, persistência e integração com o vault.
