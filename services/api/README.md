# API Spring Boot do Tailorly

Esta pasta contém a API HTTP do Tailorly. Ela oferece cadastro de usuários, login com JWT, roles, owners, operações básicas de sessões anônimas e upload de PDFs para contas. O fluxo completo de ingestão, revisão e geração descrito no [README do monorepo](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) ainda é o **fluxo alvo**.

## Estado atual

| Área | Implementado | Ainda planejado |
| --- | --- | --- |
| Contas | Cadastro, consulta, alteração de email, desativação, senha com BCrypt e roles persistidas | Recuperação de senha e exclusão de conta |
| Acesso | Login com JWT Bearer HS256, autorização por conta e `ROLE_ADMIN` | Refresh token, logout e revogação individual de JWT |
| Sessões anônimas | Registros de owner e sessão, validação de expiração e revogação administrativa | Emissão de token pelo servidor, cotas, limpeza automática e migração para conta |
| Currículo Base | Entidades JPA para documentos, versões, execuções de extração e páginas; upload de PDF para conta com deduplicação por owner; campo `activeSnapshotId` no owner, sem modelo de snapshot | Upload anônimo, execução de extração/OCR, fatos com evidência, revisão e ativação do Ground Truth |
| Geração | Nenhum endpoint | Ingestão de vaga, template Typst, PDF/Markdown e histórico |
| Infraestrutura | PostgreSQL via JPA, armazenamento privado via API S3, OpenAPI, Swagger UI e Actuator | Flyway, limpeza de arquivos e observabilidade ampliada |

O upload grava PDFs no Garage local pelo protocolo S3. A dependência Jsoup também está presente, mas ainda não há extração de vagas por URL.

## Tecnologias e estrutura

- Java 21, Spring Boot 3.5, Spring Web, Validation, Security, OAuth2 Resource Server e Spring Data JPA.
- PostgreSQL via JPA para `users`, `roles`, `user_roles`, `owners`, `anonymous_sessions`, `stored_objects`, `base_documents`, `base_versions`, `extraction_runs` e `extracted_pages`.
- Springdoc para OpenAPI/Swagger UI e Actuator para saúde.
- Pacotes em `src/main/java/com/tailorly/api`: `user`, `role`, `owner`, `session`, `objects`, `base`, `extraction` e `shared` (configuração, segurança, erros e handlers).
- Mensagens em inglês e português em `src/main/resources`; testes em `src/test`.

Os endpoints de negócio começam com `/v1`. Actuator e documentação usam os caminhos próprios das ferramentas.

Os metadados de storage podem ser registrados e consultados em `/api/stored-objects` (também disponível sob `/v1/stored-objects`). O cadastro exige `ownerId`, `purpose`, `storageKey`, `contentHash`, `mimeType` e `sizeBytes`; `expiresAt` é opcional e `deletionStatus` assume `ACTIVE` quando omitido. Essas operações exigem autenticação e só permitem acesso ao owner da própria conta ou a administradores.

O cadastro de documento apenas por metadados permanece disponível como operação interna do serviço, sem rota HTTP pública. O fluxo HTTP de criação usa o upload abaixo para garantir que o arquivo seja gravado no storage.

`POST /v1/base-documents/upload` (também `/api/base-documents/upload`) recebe `multipart/form-data` com `ownerId` e `file`. Exige Bearer e owner de conta pertencente ao usuário. A API valida um PDF não vazio de até 7 MiB, calcula SHA-256 dos bytes, procura um documento com o mesmo hash **no mesmo owner** e devolve `{ "document": ..., "reused": true }` se já houver. Um arquivo novo é gravado no bucket privado em `owners/{ownerId}/originals/{uuid}.pdf` e seus metadados são registrados em `stored_objects` e `base_documents`; a resposta tem `reused: false`. O endpoint retorna `201` nos dois casos. O nome original serve apenas como metadado; não define a chave do objeto. O upload ainda não cria versão, executa OCR ou aprova um snapshot. A requisição multipart completa tem limite de 8 MiB para incluir campos e cabeçalhos.

Se o banco registrar um documento idêntico mas o original não existir no storage, a rota retorna `409` em vez de reutilizar metadados sem arquivo. Uma falha na transação de registro dispara uma tentativa de remover o arquivo recém-enviado; a API registra no log caso essa compensação falhe.

O serviço depende da interface `ObjectStorage`; o adaptador atual usa S3. Para apontar ao Cloudflare R2, configure endpoint, região e credenciais correspondentes. Mantenha o bucket privado. `POST /v1/stored-objects` continua sendo uma rota de metadados: ela não grava bytes nem verifica a presença do objeto no storage. `PATCH /v1/stored-objects/{id}/deletion-status` só altera metadados e **não apaga o arquivo**.

Também é possível consultar documentos por owner em `GET /api/base-documents/owner/{ownerId}` ou por hash em `GET /api/base-documents/hash/{contentHash}`. A consulta global por hash exige administrador e retorna conflito quando o hash aparece em mais de um owner.

Versões de documentos são criadas por `POST /api/base-versions` (ou `/v1/base-versions`) com `ownerId`, `documentId`, `versionNumber` e `status`. O documento precisa pertencer ao owner, e o número da versão é único dentro do owner.

Uma versão pode ser consultada em `GET /api/base-versions/{id}`, ter o status alterado em `PATCH /api/base-versions/{id}/status` e avançar sua revisão em `PATCH /api/base-versions/{id}/increment-review-revision`.

Execuções de extração são registradas em `POST /api/extraction-runs` (ou `/v1/extraction-runs`) com `baseVersionId`, `pipelineFingerprint`, `pipelineVersions`, `status` e `outputObjectId` opcional. Elas podem ser consultadas, ter status alterado ou ser marcadas como falhas pelos endpoints correspondentes. O owner da versão controla o acesso.

Páginas extraídas são registradas em `POST /api/extracted-pages` (ou `/v1/extracted-pages`) com `extractionRunId`, método, objeto de texto e hash. Elas podem ser consultadas por ID, por execução ou por hash de texto; a busca global por hash exige administrador e um resultado único.

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

Depois carregue as credenciais geradas e configure a API:

```bash
set -a
source .env.garage
set +a
export DATABASE_USERNAME=tailorly
export DATABASE_PASSWORD=tailorly
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tailorly
export TAILORLY_JWT_SECRET="$(openssl rand -base64 32)"

./mvnw spring-boot:run
```

O volume antigo `tailorly_minio-data`, caso exista, não é importado pelo Garage nem removido pelo Compose. Migre os objetos e confira as referências `storageKey` no banco antes de desativar o storage antigo; não presuma que trocar o serviço migra os PDFs. O armazenamento de originais continua privado.

Para conferir o S3 sem enviar um currículo real, depois de carregar `.env.garage` como acima, execute:

```bash
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

O Compose cria o banco **`tailorly`**. Já o valor padrão de `spring.datasource.url` em `application.yml` aponta para **`tailorly_api_db`**; por isso o exemplo define `SPRING_DATASOURCE_URL`. Outra opção é criar `tailorly_api_db` no servidor e usar o valor padrão.

Gere a chave JWT uma vez para o ambiente e mantenha seu valor entre reinicializações: trocar a chave invalida todos os tokens emitidos antes. Não a grave no repositório. A configuração exige uma chave Base64 com pelo menos 32 bytes decodificados. A URL do emissor usa `http://localhost:8080` por padrão; fora do ambiente local, defina `TAILORLY_JWT_ISSUER` com a URL pública da API.

Também é possível iniciar pelo workspace do monorepo, com as mesmas variáveis de ambiente:

```bash
pnpm --filter @tailorly/api dev
```

### Configuração relevante

| Variável | Uso | Valor padrão |
| --- | --- | --- |
| `DATABASE_USERNAME` | Usuário do PostgreSQL | Obrigatória |
| `DATABASE_PASSWORD` | Senha do PostgreSQL | Obrigatória |
| `SPRING_DATASOURCE_URL` | URL JDBC; necessária para usar o Compose sem criar outro banco | `jdbc:postgresql://localhost:5432/tailorly_api_db` |
| `TAILORLY_JWT_SECRET` | Chave Base64 para assinar e validar JWTs | Obrigatória |
| `TAILORLY_JWT_ISSUER` | Emissor (`iss`) aceito pela API | `http://localhost:8080` |
| `TAILORLY_ADMIN_EMAILS` | Emails administrativos separados por vírgula | Lista vazia |
| `TAILORLY_STORAGE_ENDPOINT` | Endpoint S3 do Garage ou R2 | `http://localhost:3900` |
| `TAILORLY_STORAGE_REGION` | Região de assinatura S3; para R2, use `auto` | `garage` |
| `TAILORLY_STORAGE_BUCKET` | Bucket privado, criado antes do upload | `tailorly-private` |
| `TAILORLY_STORAGE_ACCESS_KEY` / `TAILORLY_STORAGE_SECRET_KEY` | Credenciais S3; se omitidas, usa a cadeia padrão do SDK | Vazias |
| `TAILORLY_STORAGE_PATH_STYLE` | Endereçamento por caminho para Garage/R2 | `true` |

O JWT expira após uma hora (`tailorly.security.jwt.ttl: PT1H`). O JPA usa `ddl-auto: update` e mostra SQL em desenvolvimento; ainda não existem migrações Flyway. O CORS permite `http://localhost:5173` para `/v1/**`. O Vite do frontend encaminha `/v1` para `http://localhost:8080` no ambiente local.

## Autenticação e autorização

1. `POST /v1/auth/register` cria um usuário. `POST /v1/users` também cadastra e permanece disponível.
2. `POST /v1/auth/login` valida email e senha pelo Spring Security e devolve `accessToken`, `tokenType`, `expiresAt` e `user`.
3. O cliente envia `Authorization: Bearer <accessToken>` em cada chamada protegida. HTTP Basic não está habilitado.
4. A API valida assinatura HS256, emissor e expiração. O `sub` do JWT é o UUID da conta; a conta e suas roles são consultadas no banco em cada requisição protegida. Uma conta desativada perde acesso imediatamente.

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
| `POST /v1/users` | Público | Caminho de cadastro também disponível. |
| `GET /v1/users` | Admin | Listar contas. |
| `GET /v1/users/by-email?email=...` | Admin | Consultar por email. |
| `GET /v1/users/{id}` | Própria conta ou admin | Consultar conta. |
| `PUT /v1/users/{id}` | Própria conta ou admin | Alterar email; corpo `email`. |
| `PATCH /v1/users/{id}/disable` | Própria conta ou admin | Desativar conta; retorna `204`. |
| `POST /v1/owners` | Público na configuração HTTP | Criar owner `anonymous` ou `account`; o tipo `account` exige Bearer da própria conta. |
| `GET /v1/owners/{id}` | Conta proprietária | Consultar owner de conta. |
| `PATCH /v1/owners/{id}/active-snapshot` | Conta proprietária | Definir `activeSnapshotId` com um UUID ou limpar com `{"activeSnapshotId":null}`. |
| `PATCH /v1/owners/{id}/increment-revision` | Conta proprietária | Incrementar atomicamente `revision`. |
| `POST /v1/anonymous-sessions` | Público | Criar sessão para owner anônimo; corpo `ownerId`, `tokenHash`, `expiresAt`. |
| `POST /v1/anonymous-sessions/validate` | Público | Validar `tokenHash`; retorna `valid`, e, se válido, `ownerId` e `expiresAt`. |
| `POST /v1/base-documents/upload` | Conta proprietária ou admin | Enviar PDF e registrar documento; multipart `ownerId`, `file`. |
| `GET /v1/anonymous-sessions/{id}` | Admin | Consultar sessão. |
| `PATCH /v1/anonymous-sessions/{id}/revoke` | Admin | Revogar sessão. |

`GET /actuator/health` é público. A especificação está em `GET /v3/api-docs`, e a UI em `/swagger-ui.html`. As demais rotas exigem autenticação por padrão.

### Owners e sessões anônimas: limites atuais

Um owner é uma identidade de propriedade de dados: `kind` pode ser `account` ou `anonymous`. Um owner de conta é ligado ao usuário autenticado; `userId`, quando enviado, precisa corresponder à conta. O campo `activeSnapshotId` aceita um UUID, mas **ainda não há entidade de snapshot nem validação de fatos aprovados**. Atualizá-lo não ativa um Ground Truth real.

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

As entidades `ExtractionRun` (`extraction_runs`) e `ExtractedPage` (`extracted_pages`) modelam execuções e páginas extraídas. Cada versão pode ter várias execuções, e cada execução pode ter várias páginas. `pipeline_versions` é um objeto JSON armazenado como `jsonb` no PostgreSQL. O objeto de saída da execução é opcional e exclusivo; o objeto de texto de cada página é obrigatório e exclusivo. Os vínculos são navegáveis nos dois sentidos. `StoredObject.owner` é obrigatório, e `Owner` expõe suas coleções de objetos, documentos e versões. Esses registros de extração possuem endpoints de metadados, mas ainda não há execução de OCR ou gravação dos resultados da extração no storage.

`users` guarda o hash BCrypt da senha e o estado `enabled`; `user_roles` relaciona as roles; `owners` guarda tipo, vínculo opcional com a conta, revisão, expiração e o UUID do snapshot ativo; `anonymous_sessions` guarda hash de token, expiração e revogação. `stored_objects` modela metadados de storage; cada `base_documents` exige um objeto original, e cada objeto pode ser original de no máximo um documento (`original_object_id` único). O hash do documento é único por owner. Cada documento pode originar várias `base_versions`, que usam número único por owner. Ainda não há persistência de fatos revisados ou currículos gerados.

O fluxo alvo parte do **Currículo Base revisado como fonte dos fatos**: upload com hash por conta ou sessão → extração/OCR com evidência → revisão de fatos e pendências por versão → ativação atômica de uma versão aprovada → geração a partir de um snapshot imutável e da vaga. Templates Typst devem formatar os fatos; vagas e fontes secundárias não devem criar fatos pessoais. Versões candidatas não devem substituir a versão ativa antes da revisão. O upload e a deduplicação de documentos por conta estão implementados; o reaproveitamento de OCR/fatos, a reavaliação temporal e o restante do fluxo **não estão implementados**. Também faltam autenticação segura para visitantes, TTL e limpeza do storage anônimo, migração para conta, exclusão de originais/textos/fatos/pendências/artefatos derivados e retenção configurável para contas. Não prometa exclusão completa a partir do `deletionStatus` atual.

Veja a [decisão de produto no README principal](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) para os invariantes completos.

## Comandos de desenvolvimento

Na pasta `services/api`:

```bash
./mvnw test
./mvnw compile
./mvnw clean package
```

No workspace, `pnpm --filter @tailorly/api test`, `check` e `build` executam os comandos Maven equivalentes. Os testes atuais cobrem cadastro/login, assinatura e expiração do JWT, acesso Bearer e upload/deduplicação/compensação em caso de rollback com mocks. Ainda não há testes de integração com PostgreSQL/Garage ou contrato HTTP completo.
