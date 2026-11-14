# Desenvolvimento local

Como rodar o Tailorly na sua máquina, com a infraestrutura de apoio, e como validar mudanças. Para
publicar, veja [Deploy](deploy.md). As diferenças em relação à produção estão na
[tabela de arquitetura](architecture.md#em-produção-e-em-desenvolvimento).

## Pré-requisitos

| Ferramenta     | Versão                                                                     |
| -------------- | -------------------------------------------------------------------------- |
| Node.js        | 22.18.0 ou superior (ou 24.11.0 ou superior)                               |
| pnpm           | 10.9.0 (gerenciado pelo Corepack)                                          |
| Java           | 21 (o `./mvnw` baixa o Maven sozinho)                                      |
| Docker Compose | PostgreSQL, Garage e (opcional) SearXNG locais                             |
| Tesseract 5    | Opcional: só para OCR de páginas escaneadas fora do Docker (`por` e `eng`) |

## Passo a passo

1. Instale as dependências:

   ```bash
   corepack enable
   pnpm install
   ```

2. Crie as credenciais locais do Garage (uma vez) e suba os serviços de apoio:

   ```bash
   bash services/api/init-garage-env.sh
   docker compose up -d
   ```

3. Inicie web e API:

   ```bash
   pnpm dev
   ```

   Ou cada uma separada: `pnpm --filter @tailorly/web dev` e `pnpm --filter @tailorly/api dev`.

| Serviço        | Endereço                                |
| -------------- | --------------------------------------- |
| Web            | `http://localhost:5173`                 |
| API            | `http://localhost:8080`                 |
| Health check   | `http://localhost:8080/actuator/health` |
| OpenAPI (JSON) | `http://localhost:8080/v3/api-docs`     |
| Swagger UI     | `http://localhost:8080/swagger-ui.html` |

O script `dev` da API ativa o perfil Spring `local`: banco `tailorly` com as credenciais do Compose,
chaves de JWT e de criptografia só de desenvolvimento e SearXNG em `http://127.0.0.1:8888`. Fora
desse perfil, `TAILORLY_JWT_SECRET` e `TAILORLY_SECRETS_KEY` são obrigatórias (ver
[configuração da API](../services/api/docs/configuration.md)).

## Serviços de apoio (Compose)

| Serviço       | Endereço                  | Credenciais de desenvolvimento                     |
| ------------- | ------------------------- | -------------------------------------------------- |
| PostgreSQL    | `localhost:5432/tailorly` | usuário `tailorly`, senha `tailorly`               |
| Garage S3 API | `http://localhost:3900`   | chave e segredo em `services/api/.env.garage`      |
| Garage UI     | `http://127.0.0.1:3909`   | `GARAGE_ADMIN_TOKEN` em `services/api/.env.garage` |
| SearXNG       | `http://127.0.0.1:8888`   | sem login; só responde em `127.0.0.1`              |

Essas credenciais existem apenas para o ambiente local; segredos de produção nunca entram no Git.
Se a porta `5432` estiver ocupada, use `TAILORLY_POSTGRES_PORT=5433 docker compose up -d` e
`DATABASE_URL=jdbc:postgresql://localhost:5433/tailorly` na API (com `sudo`, passe a variável com
`sudo env ...`).

### Garage (storage S3 local)

O Garage usa a imagem oficial `dxflrs/garage:v2.4.1` e atende S3 em `http://localhost:3900`. O modo de nó único cria automaticamente o bucket privado `tailorly-private` e autoriza a chave gerada em `.env.garage`. As credenciais e o segredo RPC são locais, ficam fora do Git e são preservados em reinicializações. Use `docker compose -f ../../compose.yaml exec garage /garage status` para consultar o nó. Esta configuração tem replicação 1 e serve apenas para desenvolvimento.

O `init-garage-env.sh` cria `services/api/.env.garage` com chave, segredo e token de administração
não versionados. O Garage inicia em modo de nó único, cria o bucket privado `tailorly-private` e
publica a API S3 só em `127.0.0.1`. Esta configuração tem replicação 1 e serve apenas para
desenvolvimento; ela não migra objetos de um volume antigo do MinIO.

**Garage UI.** Interface de terceiros ([Noooste/garage-ui](https://github.com/Noooste/garage-ui)) para
navegar no bucket em `http://127.0.0.1:3909`. Entre com o `GARAGE_ADMIN_TOKEN` do `.env.garage` e
abra `tailorly-private/owners/{ownerId}/originals/`. Esse token dá administração completa do Garage,
inclusive apagar objetos; não compartilhe o arquivo. Para adicioná-la a uma instalação já iniciada:

```bash
bash services/api/init-garage-env.sh
docker compose up -d --force-recreate garage garage-ui
curl --fail http://127.0.0.1:3909/health
```

**Teste rápido do S3** (sem enviar um currículo real), a partir de `services/api`:

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

O segundo comando deve imprimir `garage-ok`. Remova o objeto de teste com o mesmo `curl`, usando `-X DELETE` e a mesma URL. Para testar o endpoint da API, use um PDF próprio com `POST /v1/base-documents/upload` e o Bearer de uma conta ou de uma sessão anônima.

## Variáveis de ambiente

`services/api/.env.example` lista todas as variáveis por contexto (banco, autenticação, extração/OCR,
interpretação, storage e ferramentas auxiliares). Copie para `services/api/.env.local` (ignorado pelo
Git): o script `dev` carrega esse arquivo e usa as chaves do `.env.garage` quando as de S3 estão
vazias. Reinicie a API depois de alterar. A tabela completa está em
[Configuração da API](../services/api/docs/configuration.md).

### IA local (Ollama) ou remota

`TAILORLY_INTERPRETATION_PROVIDER` aceita:

| Valor                         | Comportamento                                                                      |
| ----------------------------- | ---------------------------------------------------------------------------------- |
| `rules` (padrão)              | Sem IA: só e-mail e telefone explícitos viram fatos.                               |
| `remote`                      | Só o provedor remoto (`TAILORLY_REMOTE_*`), sem fallback.                          |
| `ollama-only`                 | Só o Ollama local, sem nenhuma chamada remota.                                     |
| `remote-with-ollama-fallback` | Tenta o remoto e cai para o Ollama local em caso de erro (recomendado localmente). |
| `claude-cli`                  | **Só uso local:** chama o Claude Code logado nesta máquina (veja abaixo).          |
| `codex-cli`                   | **Só uso local:** chama o Codex CLI logado nesta máquina (veja abaixo).            |

`TAILORLY_REMOTE_ENDPOINT` aceita qualquer serviço compatível com Chat Completions (OpenAI, Anthropic,
DeepSeek, OpenRouter etc.). Para o Ollama, baixe o modelo configurado, por exemplo
`ollama pull qwen3:8b`. O endpoint remoto precisa ser alcançável pela API: em deploy na nuvem, um
serviço rodando só no seu computador (`localhost`) não serve.

#### Claude Code com a sua assinatura (`claude-cli`, só uso local)

Para usar o Claude sem pagar a API por requisição, a API pode chamar o Claude Code (`claude -p`)
já logado com a sua conta Pro/Max. O consumo entra nos limites da sua assinatura. **Não habilite em
deploy nem em ambiente que atenda outras pessoas:** a assinatura é pessoal e os termos não permitem
usá-la como backend para terceiros. Por isso o provider só funciona com a flag explícita:

```bash
TAILORLY_INTERPRETATION_PROVIDER=claude-cli
TAILORLY_CLAUDE_CLI_ENABLED=true
TAILORLY_CLAUDE_CLI_MODEL=sonnet   # opcional: alias (sonnet, opus, haiku) ou nome completo
```

Pré-requisito: `claude` instalado no `PATH` (ou em `TAILORLY_CLAUDE_CLI_COMMAND`) e logado com
`claude` → `/login`. A API roda o CLI sem ferramentas, MCP, configurações nem sessão salva, em um
diretório temporário, um processo por vez e com timeout (`TAILORLY_CLAUDE_CLI_TIMEOUT`, padrão
`PT3M`). `ANTHROPIC_API_KEY`/`ANTHROPIC_AUTH_TOKEN` são removidas do ambiente do processo para que
ele use a assinatura, e não a API. É mais lento que uma API (cada chamada abre um processo).

Cada etapa usa um esforço de raciocínio (`--effort`) próprio. Mais esforço dá respostas melhores,
mas é mais lento e consome mais da assinatura:

| Etapa                                | Esforço  | Variável                                            |
| ------------------------------------ | -------- | --------------------------------------------------- |
| Extração dos fatos do Currículo Base | `high`   | `TAILORLY_CLAUDE_CLI_EFFORT_INTERPRETATION`         |
| Adaptação do conteúdo à vaga         | `high`   | `TAILORLY_CLAUDE_CLI_EFFORT_CONTENT_ENRICHMENT`     |
| Análise dos requisitos da vaga       | `medium` | `TAILORLY_CLAUDE_CLI_EFFORT_JOB_ANALYSIS`           |
| Evidências complementares (links)    | `medium` | `TAILORLY_CLAUDE_CLI_EFFORT_SUPPLEMENTARY_EVIDENCE` |
| Due diligence da empresa             | `medium` | `TAILORLY_CLAUDE_CLI_EFFORT_DUE_DILIGENCE`          |
| Revisão de ATS                       | `medium` | `TAILORLY_CLAUDE_CLI_EFFORT_ATS_REVIEW`             |
| Plano de layout do currículo         | `low`    | `TAILORLY_CLAUDE_CLI_EFFORT_RESUME_LAYOUT_PLAN`     |

Valores aceitos: `low`, `medium`, `high`, `xhigh`, `max`. Uma etapa sem entrada ou com valor
inválido usa `TAILORLY_CLAUDE_CLI_EFFORT` (padrão `medium`).

**No perfil (Preferências de IA):** com `TAILORLY_CLAUDE_CLI_ENABLED=true`, a opção "Claude Code
local" aparece no seletor de provedor da conta, com modelo, tempo máximo por etapa (10 a 1800 s) e o
esforço de cada etapa. As variáveis acima viram os padrões do servidor: o perfil mostra quando um
valor difere deles e tem "Restaurar padrões". O que a conta salva tem prioridade sobre as variáveis;
uma etapa sem valor da conta usa o padrão do servidor para ela, e "Demais etapas" vale para etapas
novas sem esforço próprio. O executável (`TAILORLY_CLAUDE_CLI_COMMAND`) e a liberação continuam só no
servidor: a API recusa `claude-cli` (`400`, `error.ai_preferences.claude_cli_unavailable`) onde ele
não está habilitado, e o modelo só aceita nomes simples porque vira argumento do CLI. Sem a
liberação, a opção não aparece. Visitantes sem conta usam sempre a configuração do servidor.

#### Codex com a sua conta ChatGPT (`codex-cli`, só uso local)

Mesma ideia com o Codex CLI (`codex exec`), logado com a sua conta ChatGPT (`codex login`); o consumo
entra nos limites dessa conta. As mesmas restrições valem: **só uso local**, desligado por padrão:

```bash
TAILORLY_CODEX_CLI_ENABLED=true
TAILORLY_CODEX_CLI_COMMAND=codex   # ou o caminho completo
TAILORLY_CODEX_CLI_MODEL=          # vazio usa o modelo padrão do Codex
```

O Codex é um agente com ferramentas, então a API o roda em um diretório temporário com sandbox
somente leitura, sem aprovações, sem a configuração e as regras do usuário (`--ignore-user-config`,
`--ignore-rules`), sem sessão salva (`--ephemeral`), sem busca na web e com shell, navegador, plugins,
apps, hooks, geração de imagem e subagentes desligados (`--disable`), além de instruir o modelo a não
usar ferramentas. O Codex não tem prompt de sistema separado: as instruções vão antes da entrada, no
mesmo stdin. A resposta vem de `--output-last-message`. `OPENAI_*` e `CODEX_*` (menos `CODEX_HOME`, onde
fica o login) são removidas do ambiente do processo, para ele usar a conta e não uma chave de API. Como o
Codex repete o prompt no stderr, só as linhas `ERROR` vão para o log e para a mensagem de falha (por
exemplo, cota esgotada).

Esforços: as mesmas etapas e níveis do `claude-cli`, com variáveis `TAILORLY_CODEX_CLI_EFFORT` e
`TAILORLY_CODEX_CLI_EFFORT_<ETAPA>`; `max` vira `xhigh` (`model_reasoning_effort`), o maior nível do
Codex. No perfil, a opção "Codex local" aparece com `TAILORLY_CODEX_CLI_ENABLED=true` e tem os mesmos
ajustes; a API recusa `codex-cli` (`400`, `error.ai_preferences.codex_cli_unavailable`) onde ele não
está habilitado.

### Busca web (SearXNG) e Typst

- **SearXNG:** `docker compose up -d searxng`. No perfil `local` a API já usa
  `http://127.0.0.1:8888`; fora dele defina `TAILORLY_SEARXNG_URL`. Sem URL não há busca web nem
  due diligence.
- **Typst:** `services/api/.env.example` aponta `TAILORLY_TYPST_COMMAND` para `./bin/typst-docker`,
  que roda `ghcr.io/typst/typst:latest` pelo Docker. Instalar o CLI no `PATH` (`typst`) também serve;
  sem nenhum dos dois, `POST /v1/cv/generate` responde `503`.
- **Tesseract:** necessário para páginas escaneadas fora do Docker. Confira com
  `tesseract --version` e `tesseract --list-langs`.

## Comandos de qualidade

| Comando             | Finalidade                                                                        |
| ------------------- | --------------------------------------------------------------------------------- |
| `pnpm build`        | Gera o build de todos os workspaces.                                              |
| `pnpm test`         | Executa a suíte de testes do monorepo.                                            |
| `pnpm lint`         | Corrige automaticamente o que o Biome conseguir e verifica o restante.            |
| `pnpm check`        | Executa as checagens configuradas para os workspaces.                             |
| `pnpm api:generate` | Regenera tipos e funções HTTP TypeScript a partir do snapshot OpenAPI versionado. |
| `pnpm api:sync`     | Com a API ativa, atualiza o snapshot OpenAPI e executa o Orval.                   |

Testes por workspace: [web](../apps/web/docs/testing.md) (unitários com `node:test` e e2e com
Playwright) e [API](../services/api/README.md#testes) (JUnit; alguns usam PostgreSQL ou Tesseract
reais quando disponíveis).

### Contrato OpenAPI

O Springdoc publica as rotas `/v1/**` em `/v3/api-docs`, com IDs de operação estáveis (o Orval usa
esses IDs para nomear as funções TypeScript). Depois de alterar um controller ou DTO, inicie a API e
rode `pnpm api:sync` na raiz: ele baixa o contrato para `services/api/openapi/tailorly.json` e
regenera `apps/web/src/lib/api/generated`. Para regenerar sem a API ativa, use `pnpm api:generate`.
O código gerado é versionado e **não** deve ser editado manualmente.

## CI

`.github/workflows/ci.yml` roda em push e PR para `main`:

| Job    | O que faz                                                                                                           |
| ------ | ------------------------------------------------------------------------------------------------------------------- |
| `lint` | Biome.                                                                                                              |
| `api`  | Testes Maven contra um PostgreSQL de serviço, com o CLI real do Typst instalado no runner.                          |
| `web`  | Build e testes unitários.                                                                                           |
| `e2e`  | Playwright contra a stack completa (web, API, PostgreSQL e Garage via `compose.yaml`), com Typst e Tesseract reais. |

O deploy não passa pelo GitHub Actions: o Render e a Vercel publicam a cada push (ver [Deploy](deploy.md)).
