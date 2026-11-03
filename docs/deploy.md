# Deploy em produção

Para rodar localmente, veja [Desenvolvimento local](development.md). As diferenças entre local e
produção estão na [arquitetura](architecture.md#em-produção-e-em-desenvolvimento).

| Peça     | Serviço        | O que roda                                      |
| -------- | -------------- | ----------------------------------------------- |
| Frontend | Vercel         | Build estático do `apps/web` (SPA)              |
| API      | Render         | Container Docker da API Spring Boot             |
| Banco    | Neon Free      | PostgreSQL; o Flyway aplica migrations no start |
| Arquivos | Cloudflare R2  | PDFs, textos extraídos e currículos gerados     |
| CI       | GitHub Actions | Lint, testes Maven, build web e testes E2E      |

O arquivo [`render.yaml`](../render.yaml) define o serviço Docker da API. O Render faz o
build e o deploy automaticamente a cada push na branch conectada ao serviço e verifica
`/actuator/health`. Não há servidor, SSH, Caddy ou imagem GHCR necessários para produção.

> **Limites a conhecer.** O Vercel Hobby é para uso **não comercial**. O Render Free pode
> suspender o serviço após inatividade; o primeiro request depois disso pode demorar. O Neon
> Free também pode suspender o banco. O filesystem do Render é efêmero: todos os artefatos
> devem permanecer no Cloudflare R2, nunca no disco local do container.

## Ajustes para o free tier do Render

O plano Free do Render tem **512 MB de RAM, 0,1 CPU**, dorme após ~15 min sem tráfego e dá
750 h/mês por workspace. O [`render.yaml`](../render.yaml) já aplica:

- **Memória:** `JAVA_TOOL_OPTIONS` com `-Xmx256m`, `SerialGC`, `-Xss512k` e metaspace/code cache
  limitados. O restante da RAM fica para o Tesseract (`OMP_THREAD_LIMIT=1` na imagem) e o Typst,
  que rodam como processos filhos. Não aumente o heap sem testar OCR de um PDF de 3 páginas.
- **Threads e banco:** Tomcat com 20 threads, pool Hikari de 4 conexões com `minimum-idle=0` e
  `max-lifetime` de 4 min, para o Neon conseguir suspender o banco e não derrubar conexões ociosas.
- **Polling:** o worker de extração consulta o banco a cada 5 s (`TAILORLY_EXTRACTION_POLL_DELAY_MS`).
- **Visitantes:** no máximo 50 sessões anônimas ativas, TTL de 6 h; os demais limites (3 páginas,
  3 gerações etc.) seguem o padrão do `application.yml`.
- **Health check:** `/actuator/health/liveness`, que não depende do banco.
- **Limites por IP:** `TAILORLY_FORWARD_HEADERS_STRATEGY=native` faz a API enxergar o IP real do cliente atrás do proxy do Render; sem isso o rate limit trataria todos como uma pessoa só ([Segurança](security.md#atrás-de-um-proxy-render)).
- **Porta:** a API lê `PORT` (o Render define 10000); localmente continua 8080.

**Cold start.** Depois de dormir, o primeiro request pode levar de 1 a 2 min (JVM em 0,1 CPU).
Não use um ping de keep-alive: ele manteria o Neon acordado 24 h e estouraria as horas de
computação gratuitas dele. O frontend deve tolerar essa primeira resposta lenta.

**Jobs de limpeza.** `AnonymousCleanupJob` e `AccountRetentionJob` só rodam com o serviço
acordado; sessões anônimas expiradas são removidas no próximo despertar.

## 1. Neon (banco)

Use a connection string **direta**, sem pooler: no console do Neon, _Connect_ com
**Connection pooling** desligado (host sem `-pooler`). Flyway e Hikari dependem de estado de
sessão que o PgBouncer em modo transaction não preserva.

No Render, configure:

| Variável       | Valor                                          |
| -------------- | ---------------------------------------------- |
| `DATABASE_URL` | `jdbc:postgresql://HOST/BANCO?sslmode=require` |
| `PGHOST`       | Host direto do Neon                            |
| `PGDATABASE`   | Nome do banco                                  |
| `PGUSER`       | Usuário do Neon                                |
| `PGPASSWORD`   | Senha do Neon                                  |

Não use `POSTGRES_URL_NO_SSL` em produção. As variáveis `POSTGRES_*` geradas por integrações
da Vercel não precisam ser duplicadas: copie os dados para as variáveis acima.

## 2. Cloudflare R2 (arquivos)

Crie um bucket privado e um token S3 com **Object Read & Write** restrito a esse bucket.
Configure no Render:

```text
TAILORLY_STORAGE_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
TAILORLY_STORAGE_REGION=auto
TAILORLY_STORAGE_BUCKET=tailorly-private
TAILORLY_STORAGE_PATH_STYLE=true
TAILORLY_STORAGE_ACCESS_KEY=<Access Key ID>
TAILORLY_STORAGE_SECRET_KEY=<Secret Access Key>
```

O endpoint não inclui o nome do bucket. A API entrega os arquivos somente após verificar o
owner; não habilite acesso público no R2.

## 3. Render (API)

1. No Render, escolha **New → Blueprint** e conecte o repositório. O Render detectará
   `render.yaml` e criará o serviço `tailorly-api`.
2. Preencha no painel os valores marcados como secretos no Blueprint: conexão Neon, credenciais
   R2, `TAILORLY_JWT_SECRET`, `TAILORLY_SECRETS_KEY`, `TAILORLY_JWT_ISSUER`,
   `TAILORLY_CORS_ALLOWED_ORIGINS`, `TAILORLY_REMOTE_MODEL` e `TAILORLY_REMOTE_API_KEY`.
3. Use `TAILORLY_INTERPRETATION_PROVIDER=remote` e um endpoint/modelo/chave de provedor
   compatível com Chat Completions. Ollama não faz parte do serviço Render.
4. Para busca web (enriquecimento de projetos e due diligence), crie uma chave gratuita em
   [tavily.com](https://tavily.com) (1.000 créditos por mês) e defina `TAILORLY_TAVILY_API_KEY`.
   Deixe `TAILORLY_SEARXNG_URL` vazio: sem ele, o modo `auto` usa o Tavily (localmente o SearXNG
   continua sendo o padrão). Sem chave, a busca web fica desabilitada e a interface a esconde.
   O vault Obsidian também fica desligado em produção (não há disco do usuário no servidor), e a
   interface esconde esse recurso. A API informa o que existe em `GET /v1/features`.
5. Configure o domínio público em **Settings → Custom Domains**. Use esse domínio como
   `TAILORLY_JWT_ISSUER` e inclua o domínio da Vercel em `TAILORLY_CORS_ALLOWED_ORIGINS`.

O health check do Render usa `/actuator/health`. O container já instala Tesseract e Typst, então
extração OCR e geração de PDF não dependem de ferramentas instaladas fora da imagem.

## 4. Vercel (frontend)

1. Configure o projeto com **Root Directory = `apps/web`**. Cada push publica o frontend; o
   `vercel.json` mantém o rewrite da SPA.
2. Em _Settings → Environment Variables_, defina `VITE_API_URL=https://<domínio-da-api>` para
   Production (e Preview, se usar) e faça um redeploy. A variável entra no build.
3. No Render, `TAILORLY_CORS_ALLOWED_ORIGINS` deve incluir o domínio da Vercel e, se necessário,
   os previews (`https://tailorly-*.vercel.app`).

## Operação e rollback

- **Logs:** Render → `tailorly-api` → **Logs**.
- **Deploy manual:** Render → **Manual Deploy** → **Deploy latest commit**.
- **Rollback da API:** Render → **Deploys**, escolha um deploy anterior e use **Rollback**.
- **Rollback do frontend:** Vercel → **Deployments** → **Promote** em um deploy anterior.
- **Backups:** o Neon Free guarda histórico curto para restauração pontual. O R2 não tem
  versionamento automático; currículos gerados excluídos não voltam.
