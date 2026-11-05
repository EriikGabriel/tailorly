# Domínio de revisão

Como a interface modela a auditoria dos fatos do Currículo Base: as cinco dimensões, os campos de cada uma e as regras de pendência.

## As cinco dimensões

Definidas em `src/types/review-model.ts` como `reviewSteps`, e renderizadas na
ordem pelo seletor de etapas:

| #   | `id`         | Título na UI               | Coleção (`DraftCollection`)   |
| --- | ------------ | -------------------------- | ----------------------------- |
| 1   | `identity`   | Identidade & Contato       | campos soltos (`DraftFields`) |
| 2   | `experience` | Experiência & Voluntariado | `experience`, `volunteering`  |
| 3   | `projects`   | Projetos & Métricas        | `projects`                    |
| 4   | `education`  | Formação & Certificados    | `degrees`, `certifications`   |
| 5   | `skills`     | Competências & Idiomas     | `languages`, `technologies`   |

Cada etapa renderiza dentro de `ReviewStepShell`, que exibe um número, título,
descrição e um selo ("Aguardando revisão") e uma
**"Regra de integridade"** — texto que declara a restrição editorial daquela
dimensão. A etapa 3 (Projetos) recebe destaque visual na borda da regra.

## Campos por dimensão

Definidos como `const fields: ReviewField[]` dentro de cada arquivo em `-steps/`.
`ReviewField` = `{ key, label, type?, hint?, wide?, icon?, placeholder?,
maxLength?, options?, period? }`. O `type` escolhe o controle (em
`src/components/fields/`), e o valor guardado continua sendo o texto do fato:

| `type`          | Controle                                        | Observação                                                                                                                           |
| --------------- | ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `text` (padrão) | campo com ícone                                 |                                                                                                                                      |
| `email`         | campo de e-mail                                 | sinaliza formato inválido                                                                                                            |
| `tel`           | máscara de telefone brasileiro                  | números internacionais ficam como estão                                                                                              |
| `url`           | campo de link                                   | ícone da marca (GitHub, LinkedIn), botão "abrir" e domínio/caminho formatados                                                        |
| `month`         | calendário de mês e ano (Popover do Animate UI) | aceita `YYYY-MM` ou só `YYYY`; limpar sem abrir                                                                                      |
| `textarea`      | área que cresce com o texto                     | contador quando há `maxLength`                                                                                                       |
| `select`        | lista de opções (Base UI)                       | um valor extraído fora da lista continua selecionável                                                                                |
| `proficiency`   | nível em 5 degraus (Toggle Group do Animate UI) | reconhece "avançado", "Advanced" etc.; outro texto é mostrado e pode ser trocado                                                     |
| `period`        | início + término + interruptor "Atualmente"     | mantém o fato de situação (`Atual`/`Encerrado`, `Em andamento`/`Concluído`) em sincronia e avisa se o término for anterior ao início |

As dicas (`hint`) aparecem em um tooltip ao lado do rótulo e são lidas por
leitores de tela. Os cartões das coleções (Experiência, Projetos, Formação,
Certificações) são um Accordion do Animate UI e mostram um resumo formatado no
cabeçalho (cargo, empresa, período e duração, selos de vínculo e situação),
calculado por `base/-utils/record-summaries.ts`. Idiomas usam linhas com o
nível em degraus (`language-list.tsx`) e tecnologias, chips com contexto
opcional (`technology-chips.tsx`). A formatação (datas, período, duração,
telefone, links, iniciais, nível) fica em `src/utils/review/review-format.ts`, com
testes unitários.

| Etapa         | Chaves                                                                                       |
| ------------- | -------------------------------------------------------------------------------------------- |
| Identidade    | `name`, `headline`, `location`, `email`, `phone`, `summary`, `linkedin`, `github`, `website` |
| Experiência   | `company`, `role`, `employment`, `workMode`, `status`, `start`, `end`, `description`         |
| Voluntariado  | `organization`, `role`, `status`, `start`, `end`, `description`                              |
| Projetos      | `title`, `context`, `statement`, `metric`, `link`, `source`                                  |
| Formação      | `degree`, `institution`, `status`, `start`, `end`                                            |
| Certificações | `name`, `issuer`, `credential`, `issued`, `expires`                                          |
| Idiomas       | `language`, `proficiency`                                                                    |
| Tecnologias   | `name`, `context`                                                                            |

Registros podem ser **adicionados, editados, recolhidos e removidos**. As
alterações são editadas no workspace e salvas pela ação já existente no cartão
final. Registros existentes vêm da API; fatos novos ficam pendentes de
confirmação e alterações em fatos salvos geram decisões auditadas.

O switch **"Bounding Boxes de IA"** do painel do documento permanece desabilitado, porque a API ainda
não devolve coordenadas de evidência (ver [roadmap](../../../docs/roadmap.md)). Salvar os fatos e
aprovar o snapshot chamam a API.

## Modelo de pendência (`src/types/review.ts`)

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

`ReviewCategory` tem **sete** valores — `identity`, `employment`, `volunteering`,
`projects`, `education`, `skills`, `languages`; `languages` é categoria de
pendência separada de `skills`.

`ReviewChoice` é uma união discriminada por `result`:

| `result`      | Forma                                  | Efeito                   |
| ------------- | -------------------------------------- | ------------------------ |
| `"confirmed"` | `{ value, label, result }`             | completa sem texto extra |
| `"omitted"`   | `{ value, label, result }`             | completa; exclui o dado  |
| `"corrected"` | `{ value, label, result, correction }` | exige `CorrectionField`  |

`CorrectionField` = `{ label, type, placeholder?, min?, max?, originalValue?,
options? }`, com `type` em `text | email | month | number | select`.

## Regras em `src/utils/review/review-issues.ts`

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

| Tipo     | Regra                                                                                        |
| -------- | -------------------------------------------------------------------------------------------- |
| `month`  | `^\d{4}-(0[1-9]\|1[0-2])$` e entre `min` (padrão `"0000-01"`) e `max` (padrão: mês corrente) |
| `number` | finito, ≥ `min` (padrão 0), ≤ `max` se definido, e ≠ `originalValue`                         |
| `email`  | `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`                                                               |
| `select` | deve ser um valor de `options`                                                               |
| `text`   | qualquer texto não vazio                                                                     |

`reviewCategories` é o mapa `ReviewCategory` → rótulo em português usado nos
cabeçalhos da revisão rápida.

---
