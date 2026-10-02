# Mapa de requisitos e testes

Fonte de produto: `B01 Projects/Development/Tailorly/README.md` no second brain,
revisão de 01/10/2026. O README do monorepo e o `AGENTS.md` definem os invariantes.
Este é um mapa inicial de critérios para orientar TDD, não uma suíte completa
nem uma declaração de cobertura integral dos requisitos.

| ID | Critério observável para teste | Camada | Cobertura atual |
| --- | --- | --- | --- |
| RF16 | A revisão rápida impede continuar com correção vazia, inválida ou igual ao original; libera após todas as respostas válidas e volta a bloquear se a correção for apagada. | Web, prévia local | `e2e/quick-review.spec.ts`, tag `@RF16`. Implementado. |
| RF09 | Rejeitar arquivo vazio/formato não suportado, preservar a seleção anterior e apresentar erro acessível. Validar conteúdo/limites no servidor. | Web + API | `unit/client-state.test.ts` cobre a store local; cenário Playwright e servidor pendentes. |
| RF25 | Visitante sem histórico vê estado vazio ou demonstração identificada; exemplos não aparecem como documentos pessoais gerados. | Web | Playwright pendente; lista fictícia da home ainda precisa de correção. |
| RF26 | Respostas de seleção/versão/vaga obsoleta não alteram a revisão atual. | Web + API | Guardas locais em `unit/client-state.test.ts`; cenário Playwright e servidor pendentes. |
| RF03 | Conteúdo pessoal da geração usa exclusivamente itens aprovados do snapshot fixado. | API + geração | Pendente; não há geração real. |
| RF10 | Reenvio de conteúdo idêntico no mesmo titular reaproveita extração sem OCR adicional; outro titular tem escopo independente. | API + persistência | Pendente; não comprovar com IDs locais de seleção. |
| RF17 | Aprovação concorrente mantém no máximo um snapshot ativo e preserva o ativo anterior até concluir a candidata. | API + banco | Pendente; exige teste transacional. |
| RF21 | Após expiração, sessão perde acesso e dados temporários/derivados são removidos do servidor e storage. | API + storage | Pendente; não existe TTL implementado. |

Para novos cenários, use ID, título descritivo, tag `@RFxx` e annotation
`requirement`. Marque explicitamente se a cobertura é da interface, de contrato
simulado ou de integração real. Não criar testes vazios, `skip` ou testes que
passam automaticamente para representar requisitos pendentes.
