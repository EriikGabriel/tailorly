# Testes e TDD do Tailorly Web

Esta pasta separa os testes pelo nível em que exercitam a aplicação:

- `unit/`: regras e stores isoladas, executadas por `node:test`;
- `e2e/`: jornadas no navegador, executadas por Playwright;
- `fixtures/`: arquivos sintéticos compartilhados pelos testes;
- `requirements.md`: mapa entre critérios de produto e cobertura.

Os testes de navegador iniciam o Vite automaticamente em
`http://127.0.0.1:4173`; não precisam da API para o exemplo atual. A porta deve
estar livre.

## Executar

Na pasta `apps/web`:

```bash
pnpm test:e2e:install       # instalar Chromium, uma vez após instalar/atualizar
pnpm test:e2e              # executar o único exemplo no Chromium
pnpm test:e2e --grep @RF16  # filtrar pelo requisito
pnpm test:e2e:ui           # modo interativo para acompanhar o ciclo TDD
pnpm test:e2e:headed       # executar com janela do navegador
pnpm test:e2e:report       # abrir o último relatório HTML
pnpm test                 # stores + Playwright
```

Na raiz do monorepo, use `pnpm --filter @tailorly/web test:e2e`. Em uma máquina
Linux sem as bibliotecas necessárias ao Chromium, instale as dependências de
sistema conforme a documentação do Playwright (`playwright install --with-deps
chromium`). Isso pode exigir privilégios de administrador.

Relatórios e traces ficam em `playwright-report/` e `test-results/`, ignorados
pelo Git. Traces e screenshots são retidos apenas em falhas. Não usar currículos
reais nem credenciais nos testes; os artefatos podem capturar o conteúdo da tela.

## Requisitos como base

O mapa em [requirements.md](requirements.md) associa o requisito à regra
observável, à camada e ao teste. Os IDs seguem a nota do Tailorly no second
brain. Ao iniciar um comportamento novo, atualize primeiro o critério de aceite
local e mantenha-o coerente com a especificação do produto.

O único exemplo implementado está em
[e2e/quick-review.spec.ts](e2e/quick-review.spec.ts): **RF16**, gate da revisão
rápida. Ele navega na aplicação, seleciona um PDF sintético, informa uma vaga
fictícia, abre o diálogo e confirma uma das duas pendências. Depois verifica:

- correção vazia mantém o botão de continuidade desabilitado;
- valor original (`45000`) e valor abaixo do mínimo (`-1`) mantêm o bloqueio;
- valor válido e diferente (`50000`) libera a continuidade;
- apagar uma correção válida restaura o bloqueio;
- concluir a revisão válida navega para `/generator`.

O teste usa os dados de demonstração já existentes na interface e não injeta
stores nem substitui a função de validação. O PDF em `fixtures/` é sintético,
sem fatos profissionais reais. O requisito é verificado no **comportamento da
prévia local**. A aprovação remota, o bloqueio no servidor, a extração e a
geração de currículo seguem planejados; este exemplo não comprova essas etapas.

## Ciclo TDD para os próximos requisitos

1. Escolha um critério específico no mapa e escreva um cenário observável:
   entrada, ação, resultado esperado e caso que deve ser bloqueado.
2. Escolha `unit/<domínio>.test.ts` para uma regra ou store isolada, ou
   `e2e/<fluxo>.spec.ts` para uma jornada visível. Use o ID no título e a tag
   `@RFxx` nos testes E2E. Execute e confira que ele falha pela regra ainda
   ausente, sem erro de infraestrutura ou seletor incorreto (**red**).
3. Implemente o menor comportamento que atende ao critério e execute o teste
   novamente (**green**).
4. Refatore mantendo o teste aprovado (**refactor**) e registre o estado real
   de cobertura no mapa. Ao modificar stores, acrescente o teste de estado
   exigido pela documentação do frontend.

O exemplo atual protege uma regra que já existia; ele é uma regressão de
referência para esse ciclo, sem afirmar que foi escrito antes da implementação.

Use locators por papel/nome acessível, assertions do Playwright e esperas por
estado observável. Evite `waitForTimeout`, seletores por classes visuais,
assertions tautológicas e acesso direto ao estado React/Zustand. Cada teste deve
ter contexto isolado e dados próprios. Mocks, quando necessários, devem ser
identificados como contrato simulado; não comprovam implementação do servidor.

Regras como deduplicação por hash, ownership, ativação atômica e TTL exigem
testes de integração do servidor quando ele existir. Playwright pode exercitar
o fluxo/API, mas um teste apenas de tela não comprova a transação no banco.

## Como nomear arquivos E2E

`quick-review.spec.ts` usa o nome do **fluxo de interface**: revisão rápida.
O sufixo `.spec.ts` é a convenção do Playwright para arquivos de especificação;
ele permite que a configuração encontre os cenários nessa pasta. O arquivo pode
conter mais testes desse mesmo fluxo, como confirmar uma pendência, omitir uma
métrica ou encaminhar uma revisão extensa para `/base`.

Crie outro arquivo quando o fluxo mudar de responsabilidade ou de tela. Alguns
exemplos: `base-cv-upload.spec.ts`, `job-input.spec.ts`,
`ground-truth-approval.spec.ts` e `cv-generation.spec.ts`. Um arquivo não
precisa corresponder a um único requisito: vários requisitos podem compor a
mesma jornada. Mantenha cada caso de teste pequeno e nomeie-o pelo resultado
esperado, como `RF09: rejeitar arquivo vazio sem apagar a seleção anterior`.

Para testes unitários, use `.test.ts` e nomeie pelo módulo ou domínio testado:
`client-state.test.ts`, `review-issues.test.ts` ou `base-cv-store.test.ts`.

Referências: [configuração de servidor](https://playwright.dev/docs/test-webserver)
e [assertions](https://playwright.dev/docs/test-assertions) do Playwright.
