# API Spring Boot do Tailorly

Esta pasta contém a API HTTP do Tailorly. Ela oferece cadastro de usuários, login com JWT, roles, owners, operações básicas de sessões anônimas e upload de PDFs para contas. O fluxo completo de ingestão, revisão e geração descrito no [README do monorepo](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) ainda é o **fluxo alvo**.

## Estado atual

| Área | Implementado | Ainda planejado |
| --- | --- | --- |
| Contas | Cadastro com owner provisionado, contexto autenticado em `/v1/me`, consulta, alteração de email, desativação, senha com BCrypt e roles persistidas | Recuperação de senha e exclusão de conta |
| Acesso | Login com JWT Bearer HS256, autorização por conta e `ROLE_ADMIN` | Refresh token, logout e revogação individual de JWT |
| Sessões anônimas | Registros de owner e sessão, validação de expiração e revogação administrativa | Emissão de token pelo servidor, cotas, limpeza automática e migração para conta |
| Currículo Base | Upload por conta, deduplicação, versão candidata, fila persistente, extração PDFBox/Tesseract por página, revisão auditada e aprovação/ativação atômica de snapshots | Upload anônimo e extração estruturada automática |
| Geração | Nenhum endpoint | Ingestão de vaga, template Typst, PDF/Markdown e histórico |
| Infraestrutura | PostgreSQL via JPA e migrations Flyway, armazenamento privado via API S3, OpenAPI, Swagger UI e Actuator | Limpeza de arquivos e observabilidade ampliada |

O upload grava PDFs no Garage local pelo protocolo S3. A dependência Jsoup também está presente, mas ainda não há extração de vagas por URL.

## Tecnologias e estrutura

- Java 21, Spring Boot 3.5, Spring Web, Validation, Security, OAuth2 Resource Server e Spring Data JPA.
- PostgreSQL via JPA para contas, owners, documentos, extrações, registros/fatos/evidências, revisão e snapshots do Ground Truth.
- Springdoc para OpenAPI/Swagger UI e Actuator para saúde.
- Pacotes em `src/main/java/com/tailorly/api`, organizados por domínio de negócio: `user`, `role`, `owner`, `session`, `objects` (com `objects/storage` para o adaptador S3) e `shared` (`config`, `security`, `exception`, `handler`). `base` e `extraction` são divididos em subpacotes por agregado: `base/document`, `base/version`, `base/fact` (com `group` e `evidence`), `base/review` e `base/snapshot`; `extraction/run`, `extraction/page` e `extraction/processing` (PDFBox/OCR), com `ExtractionQueue`/`ExtractionWorker` na raiz de `extraction` por coordenarem os dois agregados. Cada pacote tem um `package-info.java` com a responsabilidade do domínio.
- Mensagens em inglês e português em `src/main/resources`; testes em `src/test`, espelhando a mesma estrutura de pacotes do código principal.

Os endpoints de negócio começam com `/v1`. Actuator e documentação usam os caminhos próprios das ferramentas.

`GET /v1/me` exige Bearer e devolve juntos o usuário autenticado e seu owner. Novos cadastros criam o owner na mesma transação; para contas antigas sem owner, a primeira chamada a `/v1/me` o cria sob bloqueio da conta. Assim o frontend não precisa persistir um `ownerId` separado nem chamar `POST /v1/owners` após cada login.

Os metadados de storage podem ser registrados e consultados em `/api/stored-objects` (também disponível sob `/v1/stored-objects`). O cadastro exige `ownerId`, `purpose`, `storageKey`, `contentHash`, `mimeType` e `sizeBytes`; `expiresAt` é opcional e `deletionStatus` assume `ACTIVE` quando omitido. Essas operações exigem autenticação e só permitem acesso ao owner da própria conta ou a administradores.

O cadastro de documento apenas por metadados permanece disponível como operação interna do serviço, sem rota HTTP pública. O fluxo HTTP de criação usa o upload abaixo para garantir que o arquivo seja gravado no storage.

`POST /v1/base-documents/upload` (também `/api/base-documents/upload`) recebe `multipart/form-data` com `ownerId` e `file`. Exige Bearer e owner de conta pertencente ao usuário. A API valida um PDF não vazio de até 7 MiB, calcula SHA-256 dos bytes e procura um documento com o mesmo hash **no mesmo owner**. Um arquivo novo é gravado no bucket privado em `owners/{ownerId}/originals/{uuid}.pdf` e registrado em `stored_objects` e `base_documents`. Ainda na transação do upload, a API cria a versão `CANDIDATE` com próximo número do owner ou recupera a versão existente do mesmo documento. A resposta `201` inclui `document`, `reused`, `version` e `versionReused`. O nome original serve apenas como metadado; não define a chave do objeto. O upload não executa OCR nem aprova um snapshot. A requisição multipart completa tem limite de 8 MiB para incluir campos e cabeçalhos.

O owner é bloqueado durante a operação para serializar uploads da mesma conta. Reenvio idêntico reutiliza documento, versão e execução compatível; marca `temporalReviewRequired: true` para revisão dos fatos temporais. O snapshot ativo não é alterado pelo upload. Versões legadas duplicadas podem existir; o upload recupera a de maior número sem criar outra. A extração estruturada continua pendente.

Se o banco registrar um documento idêntico mas o original não existir no storage, a rota retorna `409` em vez de reutilizar metadados sem arquivo. Uma falha na transação de registro dispara uma tentativa de remover o arquivo recém-enviado; a API registra no log caso essa compensação falhe.

O serviço depende da interface `ObjectStorage`; o adaptador atual usa S3. Para apontar ao Cloudflare R2, configure endpoint, região e credenciais correspondentes. Mantenha o bucket privado. `POST /v1/stored-objects` continua sendo uma rota de metadados: ela não grava bytes nem verifica a presença do objeto no storage. `PATCH /v1/stored-objects/{id}/deletion-status` só altera metadados e **não apaga o arquivo**.

Também é possível consultar documentos por owner em `GET /api/base-documents/owner/{ownerId}` ou por hash em `GET /api/base-documents/hash/{contentHash}`. A consulta global por hash exige administrador e retorna conflito quando o hash aparece em mais de um owner.

Uma versão pode ser consultada em `GET /api/base-versions/{id}` (ou `/v1/base-versions/{id}`). As rotas públicas de criação e alteração livre de status foram removidas: número e status da candidata pertencem ao servidor. A revisão e a aprovação agora usam operações de domínio; somente a aprovação troca o snapshot ativo.

O upload enfileira automaticamente uma extração na mesma transação do documento/versão. `POST /v1/extraction-runs` aceita somente `{ "baseVersionId": "..." }` e retorna `202` com uma execução nova ou reutilizada. Use essa operação para tentar novamente após uma falha. Fingerprint, versões do pipeline, status e resultados são definidos pelo servidor. As rotas de alteração livre de status e de registro manual de páginas foram removidas.

Consulte `GET /v1/extraction-runs/version/{baseVersionId}` para as execuções, da mais recente à mais antiga; `GET /v1/extraction-runs/{id}` para o status; `GET /v1/extracted-pages/run/{extractionRunId}` para páginas e métodos; e `GET /v1/extracted-pages/{id}/text` para texto UTF-8. Todas exigem acesso ao owner. Os aliases `/api` continuam disponíveis.

### Extração seletiva de PDF

O worker consulta a fila no PostgreSQL a cada 2 segundos. Ele reserva uma execução com `FOR UPDATE SKIP LOCKED` em uma transação curta, lê o PDF do Garage e processa fora da transação. PDFBox 3.0.8 extrai o texto de cada página; páginas com menos de 40 caracteres visíveis, proporção baixa de letras/números ou caracteres inválidos passam por renderização a 300 DPI e Tesseract. O método persistido é `DIRECT` ou `OCR`. Texto nativo curto é preservado quando o OCR retorna vazio. Não há extração de fatos ou aprovação nesta etapa.

Os estados da execução são `QUEUED`, `PROCESSING`, `COMPLETED` e `FAILED`. A versão candidata passa para `EXTRACTED` somente após a publicação de todas as páginas; o ponteiro do snapshot não é alterado pela extração. Falhas de OCR não publicam um documento parcialmente concluído. Páginas vazias podem coexistir com páginas úteis; um documento totalmente sem texto falha com `PDF_NO_TEXT`.

Os textos são gravados em `owners/{ownerId}/extractions/{runId}/pages/{pageNumber}.txt`. A transação final registra objetos, hashes e páginas, atualiza `pageCount` e conclui a execução atomicamente. Se houver falha, os objetos parciais recebem uma tentativa de limpeza; falhas de limpeza ou perda de conexão durante o commit exigem reconciliação operacional. Ainda não existe um coletor automático de objetos órfãos.

Execuções enfileiradas sobrevivem ao reinício. Execuções `PROCESSING` há mais de 10 minutos são marcadas `FAILED` com `WORKER_TIMEOUT` quando o worker consulta a fila novamente; uma nova solicitação permite tentar outra vez. Uma execução tardia não pode publicar resultados depois de expirada. Não há retry automático ilimitado. Uma fila criada com outra configuração do pipeline falha com `PIPELINE_CHANGED`, permitindo uma nova solicitação com a configuração atual.

O hash do pipeline inclui extrator, política de qualidade, normalização, limites, resolução e configuração/revisão do OCR. Dentro da mesma versão, uma execução `QUEUED`, `PROCESSING` ou `COMPLETED` com esse hash é reutilizada. Lock do owner e índice único parcial impedem duplicação concorrente; registros legados de metadados não são consumidos pelo worker. O worker ainda não produz fatos automaticamente. A revisão manual registra decisões versionadas; a aprovação aceita apenas versão `EXTRACTED`, bloqueia pendências obrigatórias, congela fatos confirmados/corrigidos com referência à decisão e troca o snapshot ativo atomicamente. O hash SHA-256 cobre os fatos aprovados ordenados por categoria, ordem e campo.

Para OCR local, instale o executável **Tesseract 5** e os modelos de português (`por`) e inglês (`eng`) no ambiente em que a API executa. Confira com `tesseract --version` e `tesseract --list-langs`. A dependência Maven do PDFBox não instala o Tesseract. PDFs digitais com texto suficiente funcionam sem ele; páginas que exigem OCR falham com `OCR_UNAVAILABLE` quando o executável não existe, ou `OCR_FAILED` quando os modelos/configuração impedem a execução.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `TAILORLY_EXTRACTION_ENABLED` | `true` | Ativar o worker; `false` conserva a fila sem processá-la. |
| `TAILORLY_TESSERACT_COMMAND` | `tesseract` | Caminho do executável local. |
| `TAILORLY_OCR_LANGUAGES` | `por+eng` | Modelos de idioma instalados. |
| `TAILORLY_OCR_REVISION` | `local-1` | Identificador do binário/modelos; alterar ao atualizar Tesseract ou traineddata para invalidar o cache. |

Limites iniciais: PDF de até 7 MiB e 20 páginas, renderização de até 20 milhões de pixels por página, texto de até 250 mil caracteres por página, timeout de OCR de 30 segundos e prazo de 3 minutos verificado entre páginas. O parser PDFBox roda no processo Java; esse prazo não interrompe uma chamada ao parser que esteja travada. Para produção, ainda falta isolamento do parser em processo/container com limite rígido de memória e tempo. PDFs criptografados são rejeitados.

A seleção é uma heurística por página: texto extraível suficiente pode coexistir com uma imagem contendo outros textos. A extração nativa pede ao PDFBox separação por artigos/colunas e supressão de texto sobreposto, mas isso não reconstrói todos os layouts nem detecta todo conteúdo misto. Não fornece confiança factual ou coordenadas de evidência; essas melhorias dependem de análise adicional e do MER 2. Home e `/base` consultam a execução a cada 1,5 segundo enquanto ela está na fila ou processando e mostram conclusão ou falha; o contrato atual não informa percentual nem progresso por página.

Os testes unitários geram PDFs sintéticos digitais, escaneados e mistos; a maioria simula o OCR. `PdfExtractorTest.realOcrWhenAvailable` executa uma página escaneada com cabeçalho, experiência e formação pelo Tesseract real quando o binário e o modelo `eng` estão disponíveis; caso contrário, o caso é ignorado. Para exercitá-lo, instale separadamente Tesseract 5 e `eng` no ambiente da API e execute `./mvnw -Dtest=PdfExtractorTest#realOcrWhenAvailable test`. O teste `ExtractionPersistenceTest` usa PostgreSQL real e só é habilitado com `TAILORLY_TEST_DATABASE_URL`, `TAILORLY_TEST_DATABASE_USER` e `TAILORLY_TEST_DATABASE_PASSWORD`, apontando para um banco descartável. Execute `./mvnw -Dtest=ExtractionPersistenceTest test` para validar migrations, concorrência, isolamento por owner, timeout e rollback.

## Executar localmente

### Pré-requisitos

- JDK 21, com `JAVA_HOME` configurado.
- Docker Compose para PostgreSQL e Garage locais; o wrapper `./mvnw` fornece o Maven.
- `openssl` para gerar a chave do exemplo abaixo.

Na pasta `services/api`, gere as credenciais locais uma vez e inicie os serviços:

```bash
bash init-garage-env.sh
docker compose -f ../../compose.yaml up -d postgres garage
docker compose -f ../../compose.yaml exec garage /garage status
```

Se `5432` já estiver ocupada no host, use `TAILORLY_POSTGRES_PORT=5433` no comando do Compose e configure a API com `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/tailorly`. Com `sudo`, passe a variável explicitamente: `sudo env TAILORLY_POSTGRES_PORT=5433 docker compose -f ../../compose.yaml up -d postgres garage`. Isso altera só a porta no host; o PostgreSQL continua ouvindo em `5432` dentro do contêiner.

O Garage usa a imagem oficial `dxflrs/garage:v2.4.1` e atende S3 em `http://localhost:3900`. O modo de nó único cria automaticamente o bucket privado `tailorly-private` e autoriza a chave gerada em `.env.garage`. As credenciais e o segredo RPC são locais, ficam fora do Git e são preservados em reinicializações. Use `docker compose -f ../../compose.yaml exec garage /garage status` para consultar o nó. Esta configuração tem replicação 1 e serve apenas para desenvolvimento.

### Interface visual do Garage

O serviço `garage-ui` usa a [Garage UI de Noooste](https://github.com/Noooste/garage-ui), uma interface de terceiros. Ela permite navegar no bucket e visualizar ou baixar objetos. Para adicioná-la a uma instalação já iniciada, execute na pasta `services/api`:

```bash
bash init-garage-env.sh
sudo docker compose -f ../../compose.yaml up -d --force-recreate garage garage-ui
curl --fail http://127.0.0.1:3909/health
```

Abra `http://127.0.0.1:3909`, entre com o valor de `GARAGE_ADMIN_TOKEN` em `.env.garage` e navegue em `tailorly-private/owners/{ownerId}/originals/`. O script preserva as credenciais S3 existentes e acrescenta o token administrativo se necessário. O painel e a API S3 só são publicados em `127.0.0.1`; a API administrativa do Garage (`3903`) fica acessível apenas na rede interna do Compose. O login com esse token concede administração completa do Garage, inclusive alteração e exclusão de objetos, então não compartilhe o arquivo `.env.garage`. A interface exibe as chaves dos objetos, não os nomes originais dos PDFs guardados como metadados no PostgreSQL.

Depois inicie a API:

```bash
pnpm dev
```

O script `dev` carrega de `.env.garage` somente
`TAILORLY_STORAGE_ACCESS_KEY` e `TAILORLY_STORAGE_SECRET_KEY` para o processo da
API e falha com uma instrução clara se o arquivo ou as chaves estiverem
ausentes. Reinicie a API após gerar ou alterar o arquivo. Em outros comandos
Maven ou ao executar o JAR diretamente, configure essas variáveis no ambiente.

O volume antigo `tailorly_minio-data`, caso exista, não é importado pelo Garage nem removido pelo Compose. Migre os objetos e confira as referências `storageKey` no banco antes de desativar o storage antigo; não presuma que trocar o serviço migra os PDFs. O armazenamento de originais continua privado.

Para conferir o S3 sem enviar um currículo real, carregue `.env.garage` no
terminal que executará o `curl` e depois envie um objeto de teste:

```bash
set -a
source .env.garage
set +a
printf 'garage-ok\n' > /tmp/tailorly-garage-smoke.txt
curl --fail-with-body --show-error --silent \
  --aws-sigv4 'aws:amz:garage:s3' \
  --user "$TAILORLY_STORAGE_ACCESS_KEY:$TAILORLY_STORAGE_SECRET_KEY" \
  --upload-file /tmp/tailorly-garage-smoke.txt \
  http://127.0.0.1:3900/tailorly-private/_smoke.txt
curl --fail-with-body --show-error --silent \
  --aws-sigv4 'aws:amz:garage:s3' \
  --user "$TAILORLY_STORAGE_ACCESS_KEY:$TAILORLY_STORAGE_SECRET_KEY" \
  http://127.0.0.1:3900/tailorly-private/_smoke.txt
```

O segundo comando deve imprimir `garage-ok`. Remova o objeto de teste com o mesmo `curl`, usando `-X DELETE` e a mesma URL. Para testar o endpoint da API, use um PDF próprio com `POST /v1/base-documents/upload`, Bearer e `ownerId` de uma conta autenticada; o upload anônimo ainda não está implementado.

O comando `pnpm dev` ativa o perfil Spring `local`, que usa por padrão o banco
**`tailorly`** e as credenciais do Compose. Para usar outra instância, sobrescreva
`SPRING_DATASOURCE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`.

Gere a chave JWT uma vez para o ambiente e mantenha seu valor entre reinicializações: trocar a chave invalida todos os tokens emitidos antes. Não a grave no repositório. A configuração exige uma chave Base64 com pelo menos 32 bytes decodificados. A URL do emissor usa `http://localhost:8080` por padrão; fora do ambiente local, defina `TAILORLY_JWT_ISSUER` com a URL pública da API.

Também é possível iniciar pelo workspace do monorepo:

```bash
pnpm --filter @tailorly/api dev
```

### Configuração relevante

| Variável | Uso | Valor padrão |
| --- | --- | --- |
| `DATABASE_USERNAME` | Usuário do PostgreSQL | Obrigatória; `tailorly` no perfil `local` |
| `DATABASE_PASSWORD` | Senha do PostgreSQL | Obrigatória; `tailorly` no perfil `local` |
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/tailorly_api_db`; no perfil `local`, `jdbc:postgresql://localhost:5432/tailorly` |
| `TAILORLY_JWT_SECRET` | Chave Base64 para assinar e validar JWTs | Obrigatória; o perfil `local` possui uma chave exclusiva de desenvolvimento |
| `TAILORLY_JWT_ISSUER` | Emissor (`iss`) aceito pela API | `http://localhost:8080` |
| `TAILORLY_ADMIN_EMAILS` | Emails administrativos separados por vírgula | Lista vazia |
| `TAILORLY_STORAGE_ENDPOINT` | Endpoint S3 do Garage ou R2 | `http://localhost:3900` |
| `TAILORLY_STORAGE_REGION` | Região de assinatura S3; para R2, use `auto` | `garage` |
| `TAILORLY_STORAGE_BUCKET` | Bucket privado, criado antes do upload | `tailorly-private` |
| `TAILORLY_STORAGE_ACCESS_KEY` / `TAILORLY_STORAGE_SECRET_KEY` | Credenciais S3; se omitidas, usa a cadeia padrão do SDK | Vazias |
| `TAILORLY_STORAGE_PATH_STYLE` | Endereçamento por caminho para Garage/R2 | `true` |

O JWT expira após uma hora (`tailorly.security.jwt.ttl: PT1H`). O Flyway aplica as migrations de `src/main/resources/db/migration` e o JPA usa `ddl-auto: validate`. `baseline-on-migrate` permite adotar o Flyway em bancos locais anteriormente criados pelo Hibernate: esses bancos são marcados na versão 1 e recebem as migrations posteriores. O CORS permite `http://localhost:5173` para `/v1/**`. O Vite do frontend encaminha `/v1` para `http://localhost:8080` no ambiente local.

## Autenticação e autorização

1. `POST /v1/auth/register` cria um usuário. `POST /v1/users` também cadastra e permanece disponível.
2. `POST /v1/auth/login` valida email e senha pelo Spring Security e devolve `accessToken`, `tokenType`, `expiresAt` e `user`.
3. `GET /v1/me` devolve o usuário e o owner da conta, criando o owner para contas antigas quando necessário.
4. O cliente envia `Authorization: Bearer <accessToken>` em cada chamada protegida. HTTP Basic não está habilitado.
5. A API valida assinatura HS256, emissor e expiração. O `sub` do JWT é o UUID da conta; a conta e suas roles são consultadas no banco em cada requisição protegida. Uma conta desativada perde acesso imediatamente.

O token não é guardado em cookie pela API. A configuração atual é stateless e desabilita CSRF; não existe endpoint `/v1/csrf`. Ainda não há refresh token, logout nem revogação individual de um JWT antes de sua expiração.

Todo cadastro recebe `ROLE_USER`. As roles `ROLE_USER` e `ROLE_ADMIN` são criadas na inicialização. Emails incluídos em `TAILORLY_ADMIN_EMAILS` recebem `ROLE_ADMIN` no cadastro ou na inicialização seguinte. Remover um email dessa lista **não remove automaticamente** uma role administrativa já persistida.

### Exemplo de cadastro, login e uso

Os valores abaixo são apenas exemplos de requisição; não representam dados de um visitante.

```bash
curl -i -X POST http://localhost:8080/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"pessoa@example.com","password":"senha-de-exemplo-123"}'

curl -i -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"pessoa@example.com","password":"senha-de-exemplo-123"}'
```

O cadastro retorna `201` com `id`, `email`, `enabled` e `roles`, sem a senha. A resposta de login tem o formato:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "<instante-UTC>",
  "user": {
    "id": "<uuid>",
    "email": "pessoa@example.com",
    "enabled": true,
    "roles": ["ROLE_USER"]
  }
}
```

Depois, use o JWT retornado e o UUID da conta:

```bash
curl http://localhost:8080/v1/users/<uuid> \
  -H 'Authorization: Bearer <jwt>'
```

O email de cadastro deve ser válido; a senha deve ter de 8 a 128 caracteres. Login com credenciais inválidas ou conta desativada retorna `401`. Email já cadastrado retorna `409`.

## Endpoints disponíveis

| Método e caminho | Acesso | Operação |
| --- | --- | --- |
| `POST /v1/auth/register` | Público | Cadastrar conta; corpo `email`, `password`; retorna `201` e `UserResponse`. |
| `POST /v1/auth/login` | Público | Validar credenciais; retorna JWT e `UserResponse`. |
| `GET /v1/me` | Conta autenticada | Retornar `user` e o owner único da conta; provisiona owner ausente para contas antigas. |
| `POST /v1/users` | Público | Caminho de cadastro também disponível. |
| `GET /v1/users` | Admin | Listar contas. |
| `GET /v1/users/by-email?email=...` | Admin | Consultar por email. |
| `GET /v1/users/{id}` | Própria conta ou admin | Consultar conta. |
| `PUT /v1/users/{id}` | Própria conta ou admin | Alterar email; corpo `email`. |
| `PATCH /v1/users/{id}/disable` | Própria conta ou admin | Desativar conta; retorna `204`. |
| `POST /v1/owners` | Público na configuração HTTP | Criar owner `anonymous` ou `account`; o tipo `account` exige Bearer da própria conta. |
| `GET /v1/owners/{id}` | Conta proprietária | Consultar owner de conta. |
| `PATCH /v1/owners/{id}/increment-revision` | Conta proprietária | Incrementar atomicamente `revision`. |
| `POST /v1/gt-snapshots/approve-version/{baseVersionId}` | Conta proprietária ou admin | Aprovar fatos auditados da versão extraída, gerar snapshot imutável e ativá-lo atomicamente. |
| `GET /v1/gt-snapshots/{id}` | Conta proprietária ou admin | Consultar snapshot e fatos congelados. |
| `POST /v1/anonymous-sessions` | Público | Criar sessão para owner anônimo; corpo `ownerId`, `tokenHash`, `expiresAt`. |
| `POST /v1/anonymous-sessions/validate` | Público | Validar `tokenHash`; retorna `valid`, e, se válido, `ownerId` e `expiresAt`. |
| `POST /v1/base-documents/upload` | Conta proprietária ou admin | Enviar PDF e criar ou recuperar sua versão; multipart `ownerId`, `file`. |
| `GET /v1/base-versions/{id}` | Conta proprietária ou admin | Consultar versão e marca de revisão temporal pendente. |
| `POST /v1/fact-records` | Conta proprietária ou admin | Criar agrupador; corpo `baseVersionId`, `category`, `displayOrder`. Alias `/api/fact-records`. |
| `GET /v1/fact-records/{id}` | Conta proprietária ou admin | Consultar agrupador. Alias `/api/fact-records/{id}`. |
| `GET /v1/fact-records/version/{baseVersionId}` | Conta proprietária ou admin | Listar agrupadores da versão. Alias `/api/fact-records/version/{baseVersionId}`. |
| `PATCH /v1/fact-records/{id}/display-order` | Conta proprietária ou admin | Atualizar `displayOrder`. Alias `/api/fact-records/{id}/display-order`. |
| `POST /v1/facts` | Conta proprietária ou admin | Criar fato vinculado a registro e execução opcional. Alias `/api/facts`. |
| `GET /v1/facts/{id}` | Conta proprietária ou admin | Consultar fato. Alias `/api/facts/{id}`. |
| `GET /v1/facts/record/{recordId}` | Conta proprietária ou admin | Listar fatos e evidências de um agrupador. Alias `/api/facts/record/{recordId}`. |
| `PATCH /v1/facts/{id}/review-state` | Conta proprietária ou admin | Alterar para `pendente`, `confirmado`, `corrigido` ou `omitido` e registrar decisão. Alias `/api/facts/{id}/review-state`. |
| `PATCH /v1/facts/{id}/confirm` | Conta proprietária ou admin | Confirmar valor revisado ou, se ausente, o extraído. Alias `/api/facts/{id}/confirm`. |
| `POST /v1/fact-evidence` | Conta proprietária ou admin | Criar evidência; a página precisa pertencer à versão do fato. Alias `/api/fact-evidence`. |
| `GET /v1/fact-evidence/{id}` | Conta proprietária ou admin | Consultar evidência. Alias `/api/fact-evidence/{id}`. |
| `GET /v1/fact-evidence/fact/{factId}` | Conta proprietária ou admin | Listar evidências do fato. Alias `/api/fact-evidence/fact/{factId}`. |
| `PATCH /v1/fact-evidence/{id}/locator` | Conta proprietária ou admin | Atualizar `sourceLocator`. Alias `/api/fact-evidence/{id}/locator`. |
| `GET /v1/review-issues/version/{baseVersionId}` | Conta proprietária ou admin | Listar pendências e fatos vinculados à versão. Alias `/api/review-issues/version/{baseVersionId}`. |
| `PATCH /v1/review-issues/{id}/answer` | Conta proprietária ou admin | Responder com `reviewState` e, para correções, `reviewedValue`; registra a decisão e fecha a pendência. |
| `GET /v1/anonymous-sessions/{id}` | Admin | Consultar sessão. |
| `PATCH /v1/anonymous-sessions/{id}/revoke` | Admin | Revogar sessão. |

`GET /actuator/health` é público. A especificação está em `GET /v3/api-docs`, e a UI em `/swagger-ui.html`. As demais rotas exigem autenticação por padrão.

### Owners e sessões anônimas: limites atuais

Um owner é uma identidade de propriedade de dados: `kind` pode ser `account` ou `anonymous`. Um owner de conta é ligado ao usuário autenticado; `userId`, quando enviado, precisa corresponder à conta. `activeSnapshotId` só é alterado pelo serviço de aprovação, que bloqueia o owner e a versão, valida decisões e pendências, grava os itens congelados e troca o ponteiro na mesma transação. A chave estrangeira composta impede apontar para snapshot de outro owner.

O registro de sessão anônima guarda um `tokenHash` e uma data `expiresAt` **fornecidos pelo chamador**. A validação verifica a expiração e a revogação, mas a API ainda não emite o token, não define o prazo de vida no servidor, não aplica cotas e não remove automaticamente registros expirados. Criar ou validar esse registro não autentica o visitante para as rotas protegidas. Portanto, esse conjunto de endpoints ainda não constitui o fluxo anônimo completo descrito no README do monorepo.

## Respostas de erro e idioma

Erros da API usam `timestamp`, `status`, `error`, `message`, `details` quando houver e `path`. As mensagens vêm de `messages.properties` e `messages_pt_BR.properties`; validações usam os arquivos `ValidationMessages` correspondentes. Envie `Accept-Language: pt-BR` para mensagens em português. Sem o cabeçalho, o idioma padrão é inglês.

| Status | Exemplo de situação |
| --- | --- |
| `400` | Corpo inválido, email malformado ou tipo de owner inválido. |
| `401` | Credenciais de login inválidas, token ausente, inválido ou expirado. |
| `403` | Conta autenticada tentando acessar recurso de outra conta ou rota de admin. |
| `404` | Entidade não encontrada. |
| `409` | Email ou token de sessão anônima já cadastrado. |
| `413` | Requisição multipart acima de 8 MiB; o PDF tem limite de 7 MiB. |

As chaves de mensagem são centralizadas em `shared/exception/ErrorCodes.java`. O formato de erro é definido por `ApiError`, `GlobalExceptionHandler` e `LocalizedSecurityErrorHandler`.

## Persistência e fluxo alvo de currículo

As entidades `ExtractionRun` e `ExtractedPage` persistem as execuções reais do worker e os textos por página. `pipeline_versions` guarda a configuração em `jsonb`; cada página aponta para um objeto de texto privado no Garage. Execuções antigas permanecem vinculadas à versão. A migration V5 adiciona `fact_records`, `facts`, `fact_evidence`, `review_issues`, `review_decisions`, `gt_snapshots` e `gt_snapshot_items`. A API lista fatos e evidências por agrupador, cria pendências para fatos em estado `pendente` e registra respostas como decisões versionadas. A evidência deve apontar para uma página da mesma versão/execução, e seu trecho precisa existir no texto extraído. `/base` edita os campos existentes e salva fatos na API; a revisão rápida usa as pendências persistidas. `POST /v1/gt-snapshots/approve-version/{baseVersionId}` só aprova versões extraídas sem pendências bloqueantes, cria itens imutáveis vinculados às decisões e ativa o novo snapshot na mesma transação que substitui o anterior. O hash SHA-256 cobre os fatos aprovados ordenados por categoria, ordem e campo. O pipeline ainda não classifica texto em fatos automaticamente e não há geração baseada em snapshot.

`users` guarda o hash BCrypt da senha e o estado `enabled`; `user_roles` relaciona as roles; `owners` guarda tipo, vínculo opcional com a conta, revisão, expiração e o UUID do snapshot ativo; `anonymous_sessions` guarda hash de token, expiração e revogação. `stored_objects` modela metadados de storage; cada `base_documents` exige um objeto original, e cada objeto pode ser original de no máximo um documento (`original_object_id` único). O hash do documento é único por owner. O upload cria no máximo uma nova `base_versions` por documento; versões legadas duplicadas podem continuar presentes. A numeração é única por owner. A migration V5 persiste fatos agrupados por versão, evidências por página, pendências e decisões de revisão e snapshots aprovados com seus itens congelados. A API aprova snapshots pela operação de domínio; a geração baseada neles ainda não existe.

O fluxo alvo parte do **Currículo Base revisado como fonte dos fatos**: upload → extração/OCR → fatos e revisão → snapshot aprovado → geração. Upload, deduplicação, versões, extração seletiva inicial, revisão auditada e aprovação/ativação de snapshot estão implementados. A extração estruturada automática e a geração seguem planejadas. Também faltam autenticação segura para visitantes, TTL, migração, exclusão abrangente e retenção configurável. Não prometa exclusão completa a partir do `deletionStatus` atual.

Veja a [decisão de produto no README principal](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) para os invariantes completos.

## Comandos de desenvolvimento

### Contrato OpenAPI e cliente web

O Springdoc publica somente rotas `/v1/**` em `/v3/api-docs`. Os IDs das
operações são estáveis e derivam do controller e do método Java, porque o Orval
usa esses IDs para nomear as funções TypeScript. O snapshot consumido pelo
frontend fica em `openapi/tailorly.json`.

Depois de alterar um controller ou DTO, inicie a API e rode na raiz do
monorepo:

```bash
pnpm api:sync
```

Esse comando baixa o contrato, remove a URL do servidor usada na exportação e
regenera `apps/web/src/lib/api/generated`. Para gerar novamente sem a API
ativa, usando o snapshot existente, rode `pnpm api:generate`. Os arquivos
gerados são artefatos versionados; não os edite manualmente.

Na pasta `services/api`:

```bash
./mvnw test
./mvnw compile
./mvnw clean package
```

No workspace, `pnpm --filter @tailorly/api test`, `check` e `build` executam os comandos Maven equivalentes. Há testes de autenticação, upload, versões, extração seletiva e compensação com mocks, além do teste PostgreSQL opcional descrito acima. Ainda falta uma suíte integrada com Garage e OCR real.
