# Testes da web

Há dois níveis, ambos em `apps/web/tests`:

- `unit/`: regras de utilitários e stores, executadas por `node:test`;
- `e2e/`: jornadas no navegador, executadas por Playwright com a API simulada;
- `fixtures/`: arquivos sintéticos compartilhados (PDFs de exemplo, sem dados reais).

## Testes unitários

```bash
pnpm test:stores   # node scripts/test-stores.mjs
```

`scripts/test-stores.mjs` **não adiciona dependências**. Ele descobre os `tests/unit/*.test.ts`, compila
com o `tsc` do projeto para um diretório temporário (usando os `paths` de `tsconfig.app.json`), registra
um pequeno hook que resolve os aliases (`@stores/...`, `@utils/...`) no JavaScript compilado e executa
tudo com `node --test`. O exit code é propagado e o temporário é apagado ao final. Basta criar um
`*.test.ts` novo para ele entrar na suíte.

Os testes usam `node:test` e `node:assert/strict`. Os de estado (`client-state`, `notification-store`)
rodam com `beforeEach(resetClientState)` e fixam: reset de sessão, seleção de arquivo compartilhada,
guardas de contexto obsoleto, invalidação da revisão rápida quando a vaga muda e limites do zoom.
Os demais cobrem regras puras (`review-format`, `job-url`, `job-fact-match`, `generation-gate`,
`request-guards`, `typst-editor` etc.).

**Toda mudança nas stores ou em `reset-client-state.ts` precisa vir acompanhada de um teste aqui.**

## Testes e2e (Playwright)

`playwright.config.ts` configura o Chromium, inicia um Vite isolado na porta 4173 (ela precisa estar
livre) e retém trace e screenshot só em falhas. Os cenários respondem `**/v1/**` com contratos
simulados: eles comprovam a interface, **não** a gravação no storage nem as regras do servidor.

### Executar

Na pasta `apps/web`:

```bash
pnpm test:e2e:install       # instalar Chromium, uma vez após instalar/atualizar
pnpm test:e2e              # executar os cenários no Chromium
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


## Mapa de requisitos e testes

Mapa inicial de critérios para orientar TDD, não uma declaração de cobertura integral. Os IDs vêm de
[Requisitos](../../../docs/requirements.md). Marque sempre se a cobertura é da interface, de contrato
simulado ou de integração real.

| ID   | Critério observável                                                                                                  | Camada            | Cobertura atual                                                                                                                            |
| ---- | -------------------------------------------------------------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| RF16 | A revisão rápida impede continuar com correção vazia, inválida ou igual ao original e libera após respostas válidas. | Web, API simulada | `e2e/quick-review.spec.ts` (`@RF16`).                                                                                                      |
| RF09 | Rejeitar arquivo vazio ou formato não suportado preservando a seleção; validar conteúdo e limites no servidor.       | Web + API         | `unit/client-state.test.ts`; `e2e/base-document-upload.spec.ts`; `BaseDocumentUploadServiceTest` (reuso no mesmo owner, limites anônimos). |
| RF07 | Visitante envia sem conta, vê limites e erros do servidor, migra ou descarta o currículo ao entrar.                  | Web, API simulada | `e2e/anonymous-access.spec.ts`; no servidor `AnonymousLifecycleIntegrationTest` (cota atômica, migração) e `AnonymousQuotaServiceTest`.    |
| RF10 | Os campos da revisão (calendário, vínculo, máscara de telefone, idiomas) editam e preservam o formato dos fatos.     | Web, API simulada | `e2e/base-review-steps.spec.ts` (`@RF10`).                                                                                                 |
| RF21 | Após a expiração a sessão perde acesso e os dados temporários e derivados são removidos.                             | API               | `AnonymousLifecycleIntegrationTest` (limpeza de expiradas e purga do owner, com storage em memória); remoção no S3 real, pendente.         |
| RF26 | Respostas de seleção, versão ou vaga obsoletas não alteram a revisão atual.                                          | Web               | Guardas locais em `unit/client-state.test.ts`.                                                                                             |
| RF03 | O conteúdo pessoal da geração vem só de itens aprovados do snapshot fixado.                                          | API + web         | `CvGenerationIntegrationTest`, `CvDocumentBuilderTest`, `BulletVerifierTest`; na web, `e2e/generation-gate.spec.ts` (bloqueio da geração). |
| RF17 | Aprovação concorrente mantém no máximo um snapshot ativo e preserva o ativo anterior até concluir a candidata.       | API + banco       | Pendente: teste transacional dedicado.                                                                                                     |

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

Cada arquivo leva o nome do **fluxo de interface** (`quick-review.spec.ts`, `generator-editor.spec.ts`).
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
