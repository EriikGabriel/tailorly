# Estado compartilhado da interface

Stores Zustand em `src/stores`, uma por responsabilidade, sempre consumidas por seletor de propriedade. O estado
sobrevive à navegação entre rotas na mesma aba, **sem persistência**: recarregar
ou fechar a aba descarta tudo.

Arquivos e rascunhos **não são** fatos aprovados, cache de OCR, histórico
persistente nem fonte de cotas.

| Store                    | Estado                                                                   | Responsabilidade                                                                                                                                                                      |
| ------------------------ | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `useAuthStore`           | `accessToken`, `expiresAt`, `refreshToken`                               | JWT e refresh token em `sessionStorage` para sobreviver ao reload da mesma aba; `apiFetch` tenta renovar o access token uma vez em qualquer `401`, e logout/expiração removem os dois |
| `useBaseCvStore`         | `file`, `selectionId`, `uploadedSelectionId`, `error`                    | Seleção local, ID do upload atual e guarda contra respostas atrasadas; não lista histórico remoto                                                                                     |
| `useReviewDraftStore`    | `sourceId`, `draft`, `activeStep`, `expandedRecords`                     | Rascunhos das cinco dimensões, etapa ativa, cartões expandidos — ligados a `sourceId`                                                                                                 |
| `useQuickReviewStore`    | `open`, `contextKey`, `issues`, `preview`, `answers`, `submittedPreview` | Abertura, pendências, respostas e snapshot da prévia, vinculados a `contextKey`                                                                                                       |
| `useJobDraftStore`       | `mode`, `jobUrl`, `jobText`, `revision`, `savedAt`                       | URL/texto da vaga, modo de entrada, revisão e horário da última ação de salvar (em memória)                                                                                           |
| `useDocumentViewerStore` | `zoom`                                                                   | Zoom compartilhado; `"fit"` ou número limitado a 50–200                                                                                                                               |
| `useWorkspaceStore`      | `activeView`                                                             | Modo `editor` / `preview` do workspace                                                                                                                                                |

Outras stores seguem o mesmo padrão e estão documentadas no próprio arquivo: `anonymous-session-store`
e `active-token` (sessão de visitante e qual credencial vale quando há conta e visitante ao mesmo tempo),
`job-posting-store` e `cv-generation-store` (ponteiros para a vaga e a geração abertas; o conteúdo vive
no servidor), `generation-preferences-store` (escolhas de uma geração, partindo dos padrões do perfil),
`template-draft-store` (código Typst editado e ainda não aplicado; guardado em `sessionStorage` para sobreviver ao reload, apagado em `resetClientState`), `generation-defaults-draft-store`, `owner-vault-draft-store` e `profile-settings-store` (rascunhos
das preferências do perfil, salvos ou descartados pela barra de alterações), `notification-store`
(toasts e sino) e `session-pointer` (ponteiros em `sessionStorage`). Toda edição não salva do perfil
acende a bolinha de pendência na aba correspondente.

## Seleção de arquivo

`selectFile(file)` devolve `boolean`:

- **Formato.** Aceita apenas nomes terminados em `.pdf`, alinhado ao upload atualmente disponível na API.
- **Arquivo vazio** (`size === 0`) é rejeitado.
- **Tamanho.** Arquivos acima de 7 MiB são rejeitados antes do envio.
- **Erro de seleção não apaga o arquivo anterior** — apenas grava `error`.
- **Mesma referência** (`file === get().file`) apenas limpa o erro e retorna
  `true`, preservando rascunhos. **Isto não é deduplicação por hash.**
- **Arquivo diferente** gera um `selectionId` novo com `crypto.randomUUID()` e
  **limpa** rascunhos, respostas rápidas, cartões expandidos e zoom. O
  comentário no código é explícito: _"A local selection ID is not a Ground Truth
  version or a content hash."_
- `selectFile(null)` remove a prévia local: zera `file` e `selectionId` e reaplica as limpezas. Não exclui o PDF no servidor.

DOC/DOCX permanecem parte do fluxo alvo, mas ainda não são aceitos porque a API não possui parser para esses formatos.

Na home e em `/base`, um PDF válido é selecionado para prévia local. Com JWT, o
hook compartilhado consulta o owner por `/v1/me` e envia o PDF a
`POST /v1/base-documents/upload`. A API devolve documento e versão candidata
ou reutilizada; a store guarda apenas o ID da seleção local para evitar que
uma resposta atrasada altere seu status. A interface não lista o
histórico de PDFs enviados. A troca em `/base` usa o mesmo envio da home. O
cartão distingue prévia local, envio, sucesso e falha e mostra erros da API.
Sem conta, o envio funciona como visitante: a API emite uma sessão anônima sob demanda (só ao enviar o primeiro arquivo, nunca ao abrir a página) e o token dela é usado como credencial. Um aviso na home e em `/base` explica que os dados são temporários e mostra os limites e o uso vindos do servidor. O envio
armazena o original, cria ou recupera a versão e enfileira extração no servidor.
O frontend consulta o andamento enquanto a execução está na fila ou processando
e, em `/base`, disponibiliza o texto bruto por página após a conclusão. A
extração não aprova Ground Truth. Trocar a seleção local não apaga documentos
remotos.

## Evitar chamadas desnecessárias e erros previsíveis

Regras aplicadas na camada `queries/` e em `lib/`; o servidor continua sendo a
garantia, o cliente só evita pedir o que ele recusaria ou já respondeu:

| Medida                                | Onde                                                                                     | Efeito                                                                                                                                                                                                                   |
| ------------------------------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Padrões do `QueryClient`              | `lib/query-client.ts`                                                                    | `staleTime` de 30 s, sem refetch ao focar a janela; consultas com polling ou cache infinito declaram o próprio comportamento                                                                                             |
| Política de retry                     | `lib/api/retry-policy.ts`                                                                | só falha de rede e 5xx, uma vez; 4xx (401, 403, 404, 422, 429) e erros de pré-condição nunca são repetidos                                                                                                               |
| Credencial verificada antes de chamar | `queries/*`                                                                              | consultas usam `skipToken`/`enabled`; mutations lançam `SESSION_EXPIRED_MESSAGE` sem token em vez de enviar um 401                                                                                                       |
| Sessão anônima vencida                | `stores/anonymous-session-store.ts` (`isAnonymousSessionActive`, `activeAnonymousToken`) | não é enviada, não é migrada nem encerrada no servidor (que já apagou os dados); `ensureCredential` emite outra                                                                                                          |
| Entrada da vaga validada              | `utils/job/job-posting-input.ts`, usada em `useAnalyzeJobPosting`                        | link inválido, texto vazio ou acima de 50 000 caracteres falham antes de qualquer chamada, inclusive a que emitiria a sessão anônima                                                                                     |
| Aprovar o Ground Truth                | `useApproveGroundTruth`                                                                  | exige token e versão do Currículo Base                                                                                                                                                                                   |
| Cliques repetidos                     | `use-job-target.ts`, `use-quick-review.ts`                                               | gerar ou concluir a revisão ignora novos pedidos enquanto há análise, envio ou geração em andamento                                                                                                                      |
| Polling da extração                   | `utils/extraction/extraction-polling.ts`                                                 | só a execução é consultada a cada 1,5 s; fatos e pendências são buscados **uma vez** na transição para `COMPLETED` (eles só são gravados ao concluir), com reconsulta curta se o resultado vier vazio logo após concluir |
| Template do gerador                   | `use-typst-template-draft.ts`                                                            | nome, versão e código vêm da própria geração (sem `GET /cv-templates/{id}`); o template padrão só é buscado com uma geração aberta                                                                                       |
| Prévia do Typst                       | `hooks/use-cv-preview.ts`                                                                | o mesmo código já compilado para a geração não é compilado de novo                                                                                                                                                       |
| Reenvio do mesmo arquivo              | `useSelectBaseDocumentFile`                                                              | o mesmo objeto `File` já enviado ou em envio não é reenviado                                                                                                                                                             |

O reenvio de um **arquivo idêntico escolhido de novo** continua indo ao servidor
de propósito: ele reaproveita OCR e fatos, mas reavalia os fatos sensíveis ao
tempo (ver AGENTS.md); deduplicar isso no cliente esconderia essa reavaliação.

## Guardas de contexto obsoleto

Dois mecanismos independentes impedem callbacks atrasados de sujar o rascunho atual:

- **`review-draft-store`**: `setIdentity`, `setCollection` e `setExpandedRecord`
  recebem `sourceId` e devolvem o estado **inalterado** quando
  `sourceId !== state.sourceId`. Em `setExpandedRecord` o id do registro só é
  aplicado se a fonte casar.
- **`quick-review-store`**: `setAnswer` e `savePreview` recebem `contextKey`;
  `setAnswer` também valida que a pendência existe em `issues` **e** que o
  `answer.choice` está entre as escolhas dela. `configure` zera respostas
  quando a chave muda, mas **preserva** quando a chave é a mesma — por isso as
  respostas sobrevivem a remontagens.

`ValidateDialog` monta a `contextKey` como
`JSON.stringify([selectionId, jobRevision, baseVersionId, allIssues, requiresFullReview])`
e, em cada render, só usa as respostas armazenadas se
`storedContext === contextKey`; caso contrário trata como `{}`.

## Vaga × Currículo Base

- Mudar a vaga (`setMode`, `setJobUrl`, `setJobText`) **invalida as respostas
  rápidas** (`useQuickReviewStore.reset()`) e incrementa `revision`. Valores
  idênticos são no-op (comparação campo a campo) e não invalidam nada.
- `useJobDraftStore.reset()` também incrementa `revision`, então a
  `contextKey` antiga nunca volta a casar.
- Trocar o Currículo Base **não** altera a vaga nem as preferências
  editoriais — coberto por teste.

## Preferências fora do Currículo Base

Preferências editoriais não fazem parte da revisão do Currículo Base nem são
exigidas no acesso anônimo. As opções específicas de uma geração ficam no
gerador (`useGenerationPreferencesStore`) e as configurações de conta ficam no
Profile quando houver suporte persistente; os defaults do gerador permitem
seguir sem configuração manual.

## Retomar depois de recarregar

O estado em memória some ao recarregar a aba, mas o que importa vive no servidor e é recuperado:

- **Currículo Base:** `BaseCvRestorer` (montado no `__root`) reabre, para uma conta ou sessão de visitante válida, a versão mais recente em revisão (`CANDIDATE`, `EXTRACTED` ou `ACTIVE`) de `GET /v1/base-versions/owner/{ownerId}` e baixa o PDF original em `GET /v1/base-documents/{id}/file`. Fatos, pendências e correções salvas vêm das consultas normais; nada é reenviado nem passa por OCR de novo. Edições ainda não salvas continuam sendo perdidas.
- **Gerador:** a seleção da vaga e da geração é guardada em `sessionStorage` só como ponteiro (ids; `stores/session-pointer.ts`). `useRestoreGeneratorForms` preenche o link (ou texto) da vaga a partir da versão lida e, ao abrir um currículo gerado, também reabre a vaga/versão usada e as escolhas de layout, diretrizes e ATS registradas na geração.

## Notificações (toasts e sino)

Todo processo principal avisa quando termina, com êxito ou não: leitura/análise da vaga, geração do currículo, leitura de ATS, aprovação do Ground Truth, due diligence (manual e a que a esteira inicia sozinha), coleta de evidências e a extração do Currículo Base.

- `stores/notification-store.ts` guarda o histórico (máx. 50) e os toasts visíveis (máx. 3). É **só estado de interface, em memória**: não guarda fatos nem cotas, some ao recarregar e é limpo por `resetClientState()`.
- Mutações declaram o aviso no `meta.notify` (`success`/`error`), e `lib/mutation-notifications.ts` o dispara a partir do cache de mutações; os textos ficam em `utils/notice-messages.ts`. Retorne `null` para não avisar.
- Processos sem mutação esperando (extração e due diligence automática) são vigiados por `BackgroundNotifications`, que só avisa o que viu em andamento (ou nasceu depois que o app abriu).
- `ToastViewport` (animado, pausa com mouse/foco, respeita movimento reduzido) e `NotificationBell` (sino do cabeçalho, com contador de não lidas e histórico) ficam em `components/notifications/` e são montados no `__root`.

## `resetClientState()`

Em `src/stores/reset-client-state.ts`. Limpa primeiro `useAuthStore`, inclusive
o JWT em `sessionStorage`, e então
reseta `useBaseCvStore`
(que em cascata limpa rascunhos, revisão rápida e visualizador),
`useJobDraftStore`, `useWorkspaceStore`. Depois remove
duas chaves **legadas** de `sessionStorage` —
`tailorly:job-draft` e `tailorly:review-preview` — deixadas pela antiga
implementação com drafts por componente. O `try/catch` existe porque o navegador
pode negar acesso ao storage; a memória é limpa de todo modo.

É chamado pelo fluxo cliente de saída e deve ser reutilizado em troca de conta
e expiração. `useLogout` chama `POST /v1/auth/logout` com o refresh token
guardado (melhor esforço, sem bloquear a navegação) antes de chamar
`resetClientState()`, para revogar a sessão no servidor; a função **não**
representa exclusão de arquivos ou artefatos no servidor.

## Estado que é deliberadamente local

Foco, hover, arraste, calendário aberto e medidas de animação ficam em
`useState`/`useRef` do componente, não em store.

A **URL de objeto da prévia** é recurso do visualizador: criada quando
`MasterDocumentPanel` monta, revogada na troca, remoção ou desmontagem, enquanto
o `File` permanece na store. O painel compara `preview?.file === file` para nunca
exibir o documento anterior enquanto a URL substituta é criada.

As categorias de fatos e rótulos são dados estáticos (`base/-constants/evidence.ts`). Os fatos vêm sempre da API: a interface não classifica o texto OCR em identidade, experiência ou outros tipos de fato.
