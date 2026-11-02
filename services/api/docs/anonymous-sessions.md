# Owners e acesso anônimo

## Owners

Um owner é uma identidade de propriedade de dados: `kind` pode ser `account` ou `anonymous`. Um owner de conta é ligado ao usuário autenticado; `userId`, quando enviado, precisa corresponder à conta. `activeSnapshotId` só é alterado pelo serviço de aprovação, que bloqueia o owner e a versão, valida decisões e pendências, grava os itens congelados e troca o ponteiro na mesma transação. A chave estrangeira composta impede apontar para snapshot de outro owner.

## Sessões anônimas

`POST /v1/anonymous-sessions` cria, na mesma transação, um owner `anonymous` e a sessão, e devolve um token opaco (`tlr_anon_…`, 256 bits aleatórios) **uma única vez**; o banco guarda apenas o SHA-256. Cada requisição com `Authorization: Bearer tlr_anon_…` é autenticada pelo servidor (hash, expiração e revogação) e o token nunca é tratado como JWT. O TTL vem de `tailorly.anonymous.session-ttl` e também define `owner.expires_at`, herdado pelo arquivo original e pelos textos extraídos.

**O que uma sessão anônima acessa.** Só as rotas de dados do Currículo Base em `/v1` (upload, versões, execuções, páginas, fatos, evidências, pendências e snapshots), as vagas e as gerações de currículo, e apenas do próprio owner. Usuários, owners, objetos de storage, `/v1/me`, `/v1/ai-preferences`, a migração e os aliases `/api/*` exigem conta. `OwnerAccessService` nega a uma sessão qualquer owner que não seja o seu.

**Limites aplicados no servidor** (`tailorly.anonymous.*`, variáveis `TAILORLY_ANONYMOUS_*`):

| Limite                                               | Padrão | Resposta ao exceder                                                                              |
| ---------------------------------------------------- | ------ | ------------------------------------------------------------------------------------------------ |
| Tamanho do PDF (`max-file-bytes`)                    | 3 MiB  | `400`                                                                                            |
| Páginas do PDF (`max-pages`)                         | 3      | `400`                                                                                            |
| Uploads de arquivos distintos (`max-uploads`)        | 3      | `429`                                                                                            |
| Processamentos novos (`max-extractions`)             | 3      | `429`                                                                                            |
| Gerações (`max-generations`)                         | 3      | `429` (aplicado em `POST /v1/cv/generate`; entrada idêntica reaproveita a geração e não consome) |
| Requisições por sessão (`requests-per-minute`)       | 300    | `429` com `Retry-After`                                                                          |
| Sessões emitidas por IP (`sessions-per-ip-per-hour`) | 10     | `429`                                                                                            |
| Sessões ativas no total (`max-active-sessions`)      | 1000   | `429`                                                                                            |

Os contadores ficam em `anonymous_sessions` e são incrementados de forma atômica, só abaixo do limite. Reenviar o mesmo arquivo reaproveita o documento, a extração e os fatos e **não** consome upload nem processamento. O limite de taxa é em memória e local a cada instância da API; atrás de um proxy reverso configure `server.forward-headers-strategy` para que o IP considerado seja o do cliente.

**IA no processamento anônimo.** Visitantes usam sempre a IA definida nas variáveis `TAILORLY_INTERPRETATION_*` do servidor; preferências de conta nunca se aplicam a eles.

**Limpeza.** O job `AnonymousCleanupJob` (`cleanup-interval`, padrão 5 min; `cleanup-enabled`) apaga os dados das sessões expiradas: objetos do storage, páginas, execuções, fatos, evidências, pendências, decisões, snapshots, a sessão e o owner. O visitante também pode encerrar a sessão com `DELETE /v1/anonymous-sessions/current`. Só owners anônimos podem ser apagados por esse caminho. A revogação administrativa invalida o token, mas os dados só saem na expiração.

**Migração para conta.** `POST /v1/anonymous-sessions/migrate` (conta autenticada, `sessionToken`) move para o owner da conta a cadeia do snapshot revisado ativo: documento, versão, execuções, páginas, fatos, evidências, pendências, decisões e o snapshot. Os objetos são copiados para o prefixo da conta e deixam de expirar. **Nada é reprocessado**: não há novo OCR nem nova extração. Se a conta já tem um Ground Truth ativo, a migração é recusada com `409` a menos que `replaceActive` seja `true`; nesse caso o anterior vira `SUPERSEDED`. Se a conta já tem o mesmo arquivo (mesmo hash), a versão migrada passa a usar esse documento. As demais versões do visitante e a sessão são apagadas depois da migração. Versão e snapshot são renumerados no escopo da conta.
O Blueprint do Render reduz `TAILORLY_ANONYMOUS_MAX_ACTIVE_SESSIONS` e o TTL para caber no free tier (ver [Deploy](../../../docs/deploy.md)). A regra de produto está em [Currículo Base como Ground Truth](../../../docs/product/ground-truth.md#uso-sem-conta-e-persistência).
