# Tailorly Web — `apps/web`

Interface React do Tailorly: envio do Currículo Base, auditoria dos fatos em cinco dimensões,
revisão rápida de pendências, gerador de currículos com prévia Typst ao vivo, histórico, due
diligence e perfil. Visitantes usam o app sem conta (sessão temporária no servidor); contas têm
histórico persistente.

> **Regra de leitura.** A interface nunca é fonte de verdade: fatos, cotas e cache de processamento
> vivem na API. `sessionStorage` e as stores guardam só estado de interface e ponteiros.
> Dados de exemplo nunca aparecem como currículos reais de um visitante. As regras de produto
> estão em [`docs/product`](../../docs/product/ground-truth.md); o que ainda não existe, no
> [roadmap](../../docs/roadmap.md).

## Começando

```bash
pnpm install                         # na raiz do monorepo
pnpm --filter @tailorly/web dev      # http://localhost:5173, proxy de /v1 para localhost:8080
```

Precisa da API e dos serviços de apoio rodando: veja [Desenvolvimento local](../../docs/development.md).
Em produção a variável `VITE_API_URL` define a origem da API ([Deploy](../../docs/deploy.md)).

## Documentação

| Documento                                   | Conteúdo                                                                        |
| ------------------------------------------- | ------------------------------------------------------------------------------- |
| [Estrutura](docs/structure.md)              | Árvore de `src`, onde cada tipo de código mora, **aliases de import** e rotas.  |
| [Telas](docs/screens.md)                    | Home, `/base`, gerador, histórico, due diligence, perfil e a revisão rápida.    |
| [Domínio de revisão](docs/review-domain.md) | Cinco dimensões, campos, modelo de pendência e regras de completude.            |
| [Estado da interface](docs/state.md)        | Stores Zustand, guardas de contexto, retomada após reload, notificações, reset. |
| [Design system](docs/design-system.md)      | Tokens, tipografia, movimento reduzido, camadas de componente e ícones.         |
| [Tooling](docs/tooling.md)                  | TypeScript, Biome, hooks compartilhados, comandos, cliente gerado e stack.      |
| [Testes](docs/testing.md)                   | Testes unitários, Playwright, mapa de requisitos e ciclo TDD.                   |

## Imports

Use o alias da pasta em vez de `../..` (`@stores/...`, `@utils/review/...`, `@api/client`,
`@constants/...`, `@app/...`). A tabela completa está em
[Estrutura → Aliases](docs/structure.md#aliases). Eles são declarados em `vite.config.ts`,
`tsconfig.json` e `tsconfig.app.json`, que precisam concordar.

## Antes de abrir um PR

```bash
pnpm check        # Biome
pnpm build        # vite build + tsc -b
pnpm test         # unitários + Playwright (pnpm test:e2e:install uma vez)
```

Ao implementar, mantenha esta documentação e o [`AGENTS.md`](../../AGENTS.md) sincronizados com o
comportamento efetivo.
