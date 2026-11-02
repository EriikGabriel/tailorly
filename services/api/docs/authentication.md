# Autenticação e autorização

1. `POST /v1/auth/register` cria um usuário. `POST /v1/users` também cadastra e permanece disponível.
2. `POST /v1/auth/login` valida email e senha pelo Spring Security e devolve `accessToken`, `tokenType`, `expiresAt`, `user` e `refreshToken`.
3. `GET /v1/me` devolve o usuário e o owner da conta, criando o owner para contas antigas quando necessário.
4. O cliente envia `Authorization: Bearer <accessToken>` em cada chamada protegida. HTTP Basic não está habilitado.
5. A API valida assinatura HS256, emissor e expiração. O `sub` do JWT é o UUID da conta; a conta e suas roles são consultadas no banco em cada requisição protegida. Uma conta desativada perde acesso imediatamente.

O token não é guardado em cookie pela API. A configuração atual é stateless e desabilita CSRF; não existe endpoint `/v1/csrf`.

**Refresh token e revogação.** `refreshToken` é um token opaco (prefixo `tlr_rt_`, 256 bits aleatórios), guardado como SHA-256 em `user_sessions` — o mesmo padrão de `anonymous_sessions`, mas uma conta pode ter várias sessões simultâneas (um refresh token por dispositivo/login). `POST /v1/auth/refresh` (público, recebe `{"refreshToken"}`) valida a sessão, marca a atual como revogada e emite um novo par de tokens (`rotated_from` liga a cadeia). Reapresentar um refresh token já rotacionado ou revogado é tratado como roubo: a API revoga **todas** as sessões da conta e responde `401`. `POST /v1/auth/logout` (público, mesmo corpo) revoga só a sessão daquele token. `GET /v1/me/sessions` lista as sessões ativas da conta autenticada (`id`, `createdAt`, `expiresAt`, `userAgent`) e `DELETE /v1/me/sessions/{id}` revoga uma delas — a API confere que a sessão pertence a quem está autenticado. Trocar `TAILORLY_JWT_SECRET` continua invalidando todos os access tokens já emitidos imediatamente; isso não depende do refresh token.

Todo cadastro recebe `ROLE_USER`. As roles `ROLE_USER` e `ROLE_ADMIN` são criadas na inicialização. Emails incluídos em `TAILORLY_ADMIN_EMAILS` recebem `ROLE_ADMIN` no cadastro ou na inicialização seguinte. Remover um email dessa lista **não remove automaticamente** uma role administrativa já persistida.

## Exemplo de cadastro, login e uso

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
  },
  "refreshToken": "tlr_rt_<token-opaco>"
}
```

Depois, use o JWT retornado e o UUID da conta:

```bash
curl http://localhost:8080/v1/users/<uuid> \
  -H 'Authorization: Bearer <jwt>'
```

O email de cadastro deve ser válido; a senha deve ter de 8 a 128 caracteres. Login com credenciais inválidas ou conta desativada retorna `401`. Email já cadastrado retorna `409`.

## Chave do JWT

Defina uma chave Base64 com pelo menos 32 bytes decodificados e mantenha-a estável entre reinicializações; trocar a chave invalida todos os access tokens já emitidos. Não a grave no repositório. O perfil `local` tem uma chave só de desenvolvimento.

```bash
export TAILORLY_JWT_SECRET="$(openssl rand -base64 32)"
```

Fora do ambiente local defina também `TAILORLY_JWT_ISSUER` com a URL pública da API (o padrão é `http://localhost:8080`). Para marcar administradores, liste os e-mails em `TAILORLY_ADMIN_EMAILS` (separados por vírgula): quem já está cadastrado recebe `ROLE_ADMIN` na inicialização e novos cadastros nascem com as duas roles. A lista só adiciona privilégios.

`GET /v1/me` devolve juntos o usuário autenticado e seu owner; para contas antigas sem owner, a primeira chamada o cria sob bloqueio da conta. O frontend não precisa guardar um `ownerId` separado.
