# API Spring Boot do Tailorly

Esta pasta contém a API HTTP do Tailorly. Ela já oferece cadastro de usuários, login com JWT, roles, owners e operações básicas de sessões anônimas. A ingestão e geração de currículos descritas no [README do monorepo](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) são o **fluxo alvo** e ainda não estão disponíveis como endpoints.

## Estado atual

| Área | Implementado | Ainda planejado |
| --- | --- | --- |
| Contas | Cadastro, consulta, alteração de email, desativação, senha com BCrypt e roles persistidas | Recuperação de senha e exclusão de conta |
| Acesso | Login com JWT Bearer HS256, autorização por conta e `ROLE_ADMIN` | Refresh token, logout e revogação individual de JWT |
| Sessões anônimas | Registros de owner e sessão, validação de expiração e revogação administrativa | Emissão de token pelo servidor, cotas, limpeza automática e migração para conta |
| Currículo Base | Campo `activeSnapshotId` no owner, sem modelo de snapshot | Upload, extração/OCR, fatos com evidência, revisão, versões e ativação do Ground Truth |
| Geração | Nenhum endpoint | Ingestão de vaga, template Typst, PDF/Markdown e histórico |
| Infraestrutura | PostgreSQL via JPA, OpenAPI, Swagger UI e Actuator | Flyway, storage de arquivos e observabilidade ampliada |

O MinIO sobe pelo Compose do monorepo, mas esta API ainda não lê nem grava arquivos nele. A dependência Jsoup também está presente, mas ainda não há extração de vagas por URL.

## Tecnologias e estrutura

- Java 21, Spring Boot 3.5, Spring Web, Validation, Security, OAuth2 Resource Server e Spring Data JPA.
- PostgreSQL para `users`, `roles`, `user_roles`, `owners` e `anonymous_sessions`.
- Springdoc para OpenAPI/Swagger UI e Actuator para saúde.
- Pacotes em `src/main/java/com/tailorly/api`: `user`, `role`, `owner`, `session` e `shared` (configuração, segurança, erros e handlers).
- Mensagens em inglês e português em `src/main/resources`; testes em `src/test`.

Os endpoints de negócio começam com `/v1`. Actuator e documentação usam os caminhos próprios das ferramentas.

## Executar localmente

### Pré-requisitos

- JDK 21, com `JAVA_HOME` configurado.
- Docker Compose para o PostgreSQL local; o wrapper `./mvnw` fornece o Maven.
- `openssl` para gerar a chave do exemplo abaixo.

Na pasta `services/api`, execute:

```bash
docker compose -f ../../compose.yaml up -d postgres

export DATABASE_USERNAME=tailorly
export DATABASE_PASSWORD=tailorly
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tailorly
export TAILORLY_JWT_SECRET="$(openssl rand -base64 32)"

./mvnw spring-boot:run
```

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

As chaves de mensagem são centralizadas em `shared/exception/ErrorCodes.java`. O formato de erro é definido por `ApiError`, `GlobalExceptionHandler` e `LocalizedSecurityErrorHandler`.

## Persistência e fluxo alvo de currículo

As entidades existentes são `User`, `Role`, `Owner` e `AnonymousSession`. `users` guarda o hash BCrypt da senha e o estado `enabled`; `user_roles` relaciona as roles; `owners` guarda tipo, vínculo opcional com a conta, revisão, expiração e o UUID do snapshot ativo; `anonymous_sessions` guarda hash de token, expiração e revogação. O histórico de currículos e os arquivos ainda não são persistidos por esta API.

O fluxo futuro parte do **Currículo Base revisado como fonte dos fatos**: upload com hash por conta ou sessão → extração/OCR com evidência → revisão de fatos e pendências por versão → ativação atômica de uma versão aprovada → geração a partir de um snapshot imutável e da vaga. Templates Typst devem formatar os fatos; vagas e fontes secundárias não devem criar fatos pessoais. Versões candidatas não devem substituir a versão ativa antes da revisão. Reenvios idênticos podem reaproveitar extração no mesmo escopo, com reavaliação de fatos sensíveis ao tempo. Exclusão deverá abranger também texto, fatos, pendências e artefatos gerados que contenham dados pessoais. Esses comportamentos **não estão implementados** nesta API.

Veja a [decisão de produto no README principal](../../README.md#decisão-de-produto-currículo-base-como-ground-truth) para os invariantes completos.

## Comandos de desenvolvimento

Na pasta `services/api`:

```bash
./mvnw test
./mvnw compile
./mvnw clean package
```

No workspace, `pnpm --filter @tailorly/api test`, `check` e `build` executam os comandos Maven equivalentes. Os testes atuais cobrem cadastro/login, assinatura e expiração do JWT e acesso Bearer, incluindo conta desativada. Eles usam mocks e um recorte MVC; ainda não há testes de integração com PostgreSQL ou contrato HTTP completo.
