# Currículo Base como Ground Truth

Decisão de produto que rege ingestão, extração, revisão, geração e exclusão. O Currículo Base
revisado é a **fonte dos fatos** pessoais e profissionais; os templates Typst só controlam a
apresentação. Os invariantes resumidos para quem contribui estão em [`AGENTS.md`](../../AGENTS.md).
Uma conta ou sessão anônima tem **um Ground Truth ativo** por vez; versões anteriores podem existir
para auditoria e para identificar a origem de currículos já gerados.

> **Estado.** As regras abaixo descrevem o comportamento implementado; o que está marcado como
> planejado ou listado em [Ainda não existe](#ainda-não-existe) não está no código. A geração que
> consome o snapshot está em [Geração de currículos](cv-generation.md).

## Processamento, versões e pendências

1. Ao receber um arquivo, calcular no servidor um hash do conteúdo (por exemplo, SHA-256) e consultar versões **no escopo da mesma conta ou sessão anônima**. Um arquivo idêntico já processado reutiliza a extração e os fatos; não executa OCR nem duplica registros. Uma nova visita anônima após a expiração da sessão pode exigir novo processamento. Não fazer deduplicação global entre pessoas.
2. Para arquivo novo, criar uma versão candidata. Extrair texto diretamente de PDF/DOC/DOCX quando possível e usar OCR nas páginas que precisarem. Extrair fatos estruturados com IA, preservando, para cada fato, o trecho ou página de origem, o estado de revisão e as versões do extrator, modelo, prompt e esquema.
3. Criar pendências ligadas à versão candidata para fatos ambíguos, ausentes, conflitantes ou sensíveis ao tempo. O usuário pode confirmar, corrigir ou omitir cada fato. Mudanças de arquivo não herdam respostas automaticamente; confirmações de fatos ainda atuais podem ser reaproveitadas somente após comparação e validação. Um arquivo idêntico dispensa novo OCR, mas pode exigir nova confirmação de fatos como vínculo de emprego atual.
4. Ativar a nova versão somente depois das confirmações obrigatórias, em uma operação que desativa a anterior. Enquanto a candidata é processada ou revisada, a versão ativa anterior permanece válida. A geração usa um snapshot imutável dos fatos aprovados e registra a versão do Ground Truth e da vaga usada; ela não reexecuta OCR para cada vaga.
5. Uma atualização do pipeline pode propor uma nova extração, sem sobrescrever silenciosamente fatos corrigidos ou confirmados. Saída em JSON válido ou com esquema rígido não equivale a exatidão factual; o snapshot aprovado evita que novas execuções da IA alterem o Ground Truth sem revisão.

**Progresso do processamento por IA:** a extração e a geração mostram a etapa atual com uma bolinha animada (`thinking-orbs`) e um rótulo que muda conforme a etapa, sem painel nem lista. Na extração é uma pílula discreta com o tempo decorrido; nos botões de geração, o próprio botão mostra a etapa (`AiStageLabel`, `GenerationStageLabel`). Na extração, o worker grava a etapa na execução (`extraction_runs.stage`/`stage_detail`, migration V19: abrir o PDF, extrair texto/OCR, guardar páginas, interpretação pela IA, registrar fatos), e a interface a lê no mesmo polling de status; em falha, a etapa onde parou e o motivo aparecem. Na geração, a esteira informa cada etapa (montagem, reescrita pela IA, compilação, nota de ATS, ajuste de layout por rodada, gravação) a um registro **em memória** por owner (`GenerationProgress`), consultado por `GET /v1/cv/generate/progress/{ownerId}` (`204` sem geração em curso); o registro não guarda dados do currículo e some pouco depois de terminar. Com várias instâncias da API, a consulta precisa chegar à instância que está gerando. A leitura da vaga e a busca de evidências aparecem como uma etapa só, porque não informam sub-etapas. Planejado: sub-etapas na leitura da vaga.

**Corrigir depois de aprovar:** uma versão ativa continua editável em `/base`, mas o snapshot ativo é uma cópia congelada, então salvar um rascunho **não** muda o que o gerador lê. Só "Salvar e aprovar" cria um novo snapshot e o torna ativo (a mesma barra flutuante que aparece a cada alteração oferece salvar rascunho, salvar e aprovar ou descartar). Aprovar sem nenhuma mudança nos fatos reaproveita o snapshot atual.

**Trocar**, **desativar** e **excluir** são operações diferentes, todas implementadas. Trocar ativa uma nova versão após revisão. Desativar (`PATCH /v1/owners/{id}/deactivate-snapshot`) zera o Ground Truth ativo do owner e marca a versão que estava `ACTIVE` como `DEACTIVATED`, sem apagar nada: snapshot, fatos, pendências e as gerações já feitas continuam disponíveis para auditoria. Excluir (`DELETE /v1/base-versions/{id}`) remove o arquivo original (se nenhuma outra versão do mesmo documento sobrar), o texto extraído, os fatos, as evidências, as pendências, o(s) snapshot(s) do Ground Truth aprovados a partir dela **e os currículos gerados a partir desses snapshots**, porque contêm os mesmos dados pessoais; a interface avisa isso antes de confirmar. Se a versão excluída era o Ground Truth ativo, a exclusão também desativa (`active_snapshot_id` fica nulo). Cada geração também pode continuar sendo excluída individualmente (`DELETE /v1/cv/generations/{id}`) sem afetar a versão que a originou, e a purga de visitantes continua apagando tudo pela sessão. Para contas, uma política de retenção agendada (`AccountRetentionJob`, `tailorly.account.*`) aplica a mesma exclusão de dados pessoais a contas desativadas (`PATCH /v1/users/{id}/disable`) há mais do que `retention-period` (padrão 30 dias) — a linha de `users`/`owners` continua existindo, desativada, só sem os dados pessoais.

## Uso sem conta e persistência

| Aspecto      | Sessão anônima                                                                                      | Conta autenticada                             |
| ------------ | --------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| Identidade   | Identificador de sessão assinado no servidor; sem cadastro.                                         | Identidade da conta.                          |
| Estado       | Arquivo temporário, extração, fatos, pendências e snapshot aprovado com expiração automática (TTL). | Versões, revisões e histórico persistentes.   |
| Deduplicação | Hash reutilizado somente enquanto a sessão existir.                                                 | Hash reutilizado entre envios da mesma conta. |
| Limites      | Cotas de tamanho e páginas, processamento, gerações e taxa de requisições verificadas no servidor.  | Limites do plano ou da conta.                 |
| Fim do ciclo | Expiração ou remoção apaga dados temporários; não há histórico durável.                             | Exclusão explícita e política de retenção.    |

A sessão anônima pode usar armazenamento temporário no servidor, mesmo sem criar um usuário no banco relacional. `sessionStorage` no navegador guarda apenas estado de interface; não é fonte confiável para cotas, cache de OCR ou fatos aprovados. A API protege credenciais e aplica limites. Se a pessoa criar conta antes da expiração, oferecer a migração do snapshot já processado e revisado para a conta, sem repetir OCR. Sem migração, os dados expiram. A interface de “Últimos Currículos Gerados” deve mostrar um estado vazio ou de prévia para visitantes sem histórico, nunca exemplos como se fossem documentos reais da pessoa.

## Ainda não existe

- DOC/DOCX: só PDF é aceito, porque a API não tem parser para os outros formatos.
- Coordenadas de evidência (bounding boxes) e a evidência desenhada sobre o PDF; hoje a evidência
  aponta para a página e o trecho do texto extraído.
- Recuperação de senha e exclusão da própria conta pela interface.
- Coletor automático de objetos órfãos no storage.

Veja o [roadmap](../roadmap.md) para o restante.

## Onde ver os detalhes

- Modelo de dados: [arquitetura](../architecture.md#modelo-de-dados).
- Extração, OCR e formato dos fatos: [extração de PDF](../../services/api/docs/extraction.md).
- Sessões anônimas, limites e migração: [acesso anônimo](../../services/api/docs/anonymous-sessions.md).
- Rotas: [endpoints da API](../../services/api/docs/endpoints.md).
