# Configuração da API

Toda a configuração vem de `src/main/resources/application.yml` com valores sobrescritíveis por
variáveis de ambiente. O perfil Spring `local` (ativado por `pnpm dev`) acrescenta padrões só de
desenvolvimento: banco `tailorly` com as credenciais do Compose, chaves de JWT e de criptografia
conhecidas e SearXNG em `http://127.0.0.1:8888`. Em qualquer outro perfil, `TAILORLY_JWT_SECRET` e
`TAILORLY_SECRETS_KEY` são obrigatórias. O modelo comentado está em `services/api/.env.example`.

## Servidor e banco

| Variável                      | Uso                                                                | Padrão                                      |
| ----------------------------- | ------------------------------------------------------------------ | ------------------------------------------- |
| `PORT`                        | Porta HTTP (o Render a define)                                     | `8080`                                      |
| `DATABASE_URL`                | URL JDBC do PostgreSQL (no Neon, `?sslmode=require` e host direto) | `jdbc:postgresql://localhost:5432/tailorly` |
| `PGUSER` / `PGPASSWORD`       | Credenciais do PostgreSQL                                          | `tailorly` / `tailorly`                     |
| `TAILORLY_DB_POOL_SIZE`       | Máximo de conexões do pool Hikari                                  | `10`                                        |
| `TAILORLY_DB_MIN_IDLE`        | Conexões ociosas mantidas (`0` deixa o Neon suspender)             | `10`                                        |
| `TAILORLY_DB_IDLE_TIMEOUT_MS` | Tempo até fechar uma conexão ociosa                                | `600000`                                    |
| `TAILORLY_DB_MAX_LIFETIME_MS` | Vida máxima de uma conexão                                         | `1800000`                                   |
| `TAILORLY_TOMCAT_MAX_THREADS` | Threads do Tomcat                                                  | `200`                                       |
| `TAILORLY_API_DOCS_ENABLED`   | Liga OpenAPI/Swagger UI (desligue em produção)                     | `true`                                      |

Os padrões de pool e threads são os do Spring; o Blueprint do Render os reduz para o free tier
(ver [Deploy](../../../docs/deploy.md#ajustes-para-o-free-tier-do-render)).

## Rate limit

Detalhes e justificativa em [Segurança](../../../docs/security.md). Os contadores são em memória e por
instância.

| Variável                                          | Uso                                                                            | Padrão  |
| ------------------------------------------------- | ------------------------------------------------------------------------------ | ------- |
| `TAILORLY_FORWARD_HEADERS_STRATEGY`               | `native` atrás de proxy reverso (Render) para usar o IP real; `none` sem proxy | `none`  |
| `TAILORLY_RATE_LIMIT_ENABLED`                     | Liga os limites por IP e por dono (os do acesso anônimo são à parte)           | `true`  |
| `TAILORLY_RATE_LIMIT_IP_PER_MINUTE`               | Requisições por IP, qualquer rota (exceto health check)                        | `600`   |
| `TAILORLY_RATE_LIMIT_AUTH_PER_MINUTE`             | Login, refresh e logout por IP                                                 | `30`    |
| `TAILORLY_RATE_LIMIT_REGISTER_PER_HOUR`           | Cadastros por IP                                                               | `10`    |
| `TAILORLY_RATE_LIMIT_LOGIN_FAILURES_PER_IP_EMAIL` | Falhas de login toleradas por IP + e-mail                                      | `5`     |
| `TAILORLY_RATE_LIMIT_LOGIN_FAILURES_PER_EMAIL`    | Falhas de login toleradas por e-mail, em todos os IPs                          | `20`    |
| `TAILORLY_RATE_LIMIT_LOGIN_FAILURE_WINDOW`        | Janela de contagem das falhas (duração ISO-8601)                               | `PT15M` |
| `TAILORLY_RATE_LIMIT_ACCOUNT_PER_MINUTE`          | Requisições por conta autenticada                                              | `600`   |
| `TAILORLY_RATE_LIMIT_HEAVY_PER_MINUTE`            | Rotas que usam IA, páginas externas ou PDF, por dono                           | `15`    |
| `TAILORLY_RATE_LIMIT_PREVIEW_PER_MINUTE`          | Prévia do Typst, por dono                                                      | `60`    |

## Autenticação e acesso

| Variável                        | Uso                                                            | Padrão                                        |
| ------------------------------- | -------------------------------------------------------------- | --------------------------------------------- |
| `TAILORLY_JWT_SECRET`           | Chave Base64 (≥ 32 bytes) para assinar e validar JWTs          | Obrigatória; o perfil `local` tem uma própria |
| `TAILORLY_JWT_ISSUER`           | Emissor (`iss`) aceito pela API; use a URL pública da API      | `http://localhost:8080`                       |
| `TAILORLY_REFRESH_TTL`          | Validade do refresh token                                      | `P30D`                                        |
| `TAILORLY_SECRETS_KEY`          | Criptografa as chaves de IA guardadas por conta                | Obrigatória; o perfil `local` tem uma própria |
| `TAILORLY_ADMIN_EMAILS`         | E-mails com `ROLE_ADMIN`, separados por vírgula                | Vazio                                         |
| `TAILORLY_CORS_ALLOWED_ORIGINS` | Origens do frontend aceitas em `/v1/**`, separadas por vírgula | `http://localhost:5173`                       |

O JWT expira após uma hora (`tailorly.security.jwt.ttl`). O CORS inclui `http://localhost:5173` por
padrão; o e2e do Playwright serve o build em `http://127.0.0.1:4173`, e a CI acrescenta essa origem.

## Storage

| Variável                                                      | Uso                                         | Padrão                  |
| ------------------------------------------------------------- | ------------------------------------------- | ----------------------- |
| `TAILORLY_STORAGE_ENDPOINT`                                   | Endpoint S3 (Garage local ou R2)            | `http://localhost:3900` |
| `TAILORLY_STORAGE_REGION`                                     | Região de assinatura; no R2, `auto`         | `garage`                |
| `TAILORLY_STORAGE_BUCKET`                                     | Bucket privado, criado antes do upload      | `tailorly-private`      |
| `TAILORLY_STORAGE_ACCESS_KEY` / `TAILORLY_STORAGE_SECRET_KEY` | Credenciais S3; vazias usam a cadeia do SDK | Vazias                  |
| `TAILORLY_STORAGE_PATH_STYLE`                                 | Endereçamento por caminho (Garage/R2)       | `true`                  |

## Extração e IA

| Variável                                            | Uso                                                                        | Padrão                                                               |
| --------------------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| `TAILORLY_EXTRACTION_ENABLED`                       | Liga o worker; `false` conserva a fila sem processá-la                     | `true`                                                               |
| `TAILORLY_EXTRACTION_POLL_DELAY_MS`                 | Intervalo de consulta da fila                                              | `2000`                                                               |
| `TAILORLY_TESSERACT_COMMAND`                        | Executável do Tesseract                                                    | `tesseract`                                                          |
| `TAILORLY_OCR_LANGUAGES`                            | Modelos de idioma instalados                                               | `por+eng`                                                            |
| `TAILORLY_OCR_REVISION`                             | Identificador do binário/modelos; mude ao atualizar para invalidar o cache | `local-1`                                                            |
| `TAILORLY_INTERPRETATION_PROVIDER`                  | `rules`, `remote`, `ollama-only`, `remote-with-ollama-fallback`, `claude-cli` ou `codex-cli` (os dois só uso local) | `rules`                                          |
| `TAILORLY_CLAUDE_CLI_ENABLED`                       | Libera o provider `claude-cli` (Claude Code logado nesta máquina; consome a sua assinatura). Nunca em deploy | `false`                        |
| `TAILORLY_CLAUDE_CLI_COMMAND` / `_MODEL` / `_TIMEOUT` | Executável, modelo e timeout do `claude-cli`                             | `claude` / `sonnet` / `PT3M`                                         |
| `TAILORLY_CLAUDE_CLI_EFFORT` / `_EFFORT_<ETAPA>`    | Esforço (`low`…`max`) padrão e por etapa do `claude-cli`; etapas em [Desenvolvimento local](../../../docs/development.md) | `medium`; por etapa: `high` (extração, adaptação), `low` (layout), `medium` (demais) |
| `TAILORLY_CODEX_CLI_ENABLED`                        | Libera o provider `codex-cli` (Codex CLI logado com a conta ChatGPT nesta máquina; consome os limites dessa conta). Nunca em deploy | `false` |
| `TAILORLY_CODEX_CLI_COMMAND` / `_MODEL` / `_TIMEOUT` | Executável, modelo (vazio = padrão do Codex) e timeout do `codex-cli`   | `codex` / vazio / `PT3M`                                             |
| `TAILORLY_CODEX_CLI_EFFORT` / `_EFFORT_<ETAPA>`     | Esforço padrão e por etapa do `codex-cli`; mesmas etapas do `claude-cli`, `max` vira `xhigh` | Os mesmos do `claude-cli`                         |
| `TAILORLY_REMOTE_ENDPOINT`                          | Qualquer serviço compatível com Chat Completions                           | `https://api.openai.com/v1/chat/completions`                         |
| `TAILORLY_REMOTE_MODEL` / `TAILORLY_REMOTE_API_KEY` | Modelo e chave do provedor remoto                                          | Vazios                                                               |
| `TAILORLY_OLLAMA_ENDPOINT` / `_MODEL` / `_API_KEY`  | Ollama local                                                               | `http://127.0.0.1:11434/v1/chat/completions` / `qwen3:8b` / `ollama` |

## Visitantes e contas

| Variável                            | Uso                                                                                         | Padrão     |
| ----------------------------------- | ------------------------------------------------------------------------------------------- | ---------- |
| `TAILORLY_ANONYMOUS_*`              | Limites, TTL e limpeza do acesso anônimo ([tabela](anonymous-sessions.md#sessões-anônimas)) | ver tabela |
| `TAILORLY_ACCOUNT_RETENTION_PERIOD` | Quanto tempo uma conta desativada mantém os dados                                           | `P30D`     |
| `TAILORLY_ACCOUNT_CLEANUP_INTERVAL` | Intervalo do job de retenção                                                                | `P1D`      |
| `TAILORLY_ACCOUNT_CLEANUP_ENABLED`  | Liga o job de retenção                                                                      | `true`     |

## Geração, busca e vault

| Variável                   | Uso                                                                                                      | Padrão                                   |
| -------------------------- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| `TAILORLY_TYPST_COMMAND`   | CLI do Typst que compila o PDF; localmente `./bin/typst-docker`; sem ele `POST /v1/cv/generate` dá `503` | `typst`                                  |
| `TAILORLY_WEB_SEARCH_PROVIDER` | `auto` (SearXNG se houver URL, senão Tavily), `searxng` ou `tavily`                                  | `auto`                                   |
| `TAILORLY_SEARXNG_URL`     | SearXNG para busca web e due diligence (desenvolvimento local)                                           | Vazio (`local`: `http://127.0.0.1:8888`) |
| `TAILORLY_TAVILY_API_KEY`  | Chave da API do Tavily (plano gratuito) para busca web em produção; sem chave nem SearXNG, a busca web fica desligada | Vazio                          |
| `TAILORLY_VAULT_ENABLED`   | Exporta os currículos gerados para o vault da conta                                                      | `false`                                  |
| `TAILORLY_VAULT_ROOT`      | Pasta raiz permitida para os vaults (um por conta); vazio desliga leitura e exportação                   | Vazio                                    |
| `TAILORLY_VAULT_SUBFOLDER` | Subpasta do vault onde os artefatos são copiados                                                         | `B03 Resources/CVs`                      |

## Observações

O Flyway aplica as migrations de `src/main/resources/db/migration` e o JPA usa `ddl-auto: validate`.
`baseline-on-migrate` permite adotar o Flyway em bancos locais criados antes pelo Hibernate: eles
são marcados na versão 1 e recebem as migrations seguintes. Mensagens de erro em inglês e português
ficam em `messages*.properties` e `ValidationMessages*.properties`.
