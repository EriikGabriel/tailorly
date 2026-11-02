# Deploy em produção (camada gratuita)

| Peça       | Serviço                                   | O que roda                                              |
| ---------- | ----------------------------------------- | ------------------------------------------------------- |
| Frontend   | Vercel Hobby                              | Build estático do `apps/web` (SPA)                      |
| API        | Oracle Cloud Always Free (Ampere A1, ARM) | Container da API + Caddy (HTTPS) + SearXNG              |
| Banco      | Neon Free                                 | PostgreSQL; o Flyway aplica as migrations no start      |
| Arquivos   | Cloudflare R2                             | PDFs originais, texto extraído e currículos gerados     |
| CI/CD      | GitHub Actions + integração Git da Vercel | `ci.yml` testa; `deploy.yml` publica a API; a Vercel publica o front a cada push |

Fluxo: um push na `main` roda o **CI**. Se passar, o **Deploy** gera a imagem `linux/arm64` da API no
GHCR, atualiza a VM por SSH e espera `/actuator/health` responder `UP`. Também dá para disparar
manualmente em *Actions → Deploy → Run workflow*. O frontend é publicado pela **integração Git da
Vercel** a cada push, de forma independente: numa mudança que precise de API nova, o front pode ir ao ar
alguns minutos antes dela.

> **Limites a conhecer.** O Vercel Hobby é para uso **não comercial**. A Oracle pode recuperar
> instâncias Always Free ociosas (CPU, rede e memória muito baixos por 7 dias). O Neon Free suspende o
> banco sem uso e a primeira consulta depois disso leva alguns segundos. O R2 Free cobre 10 GB.

## 1. Neon (banco)

Criado pela integração de Storage da Vercel (ou direto no console do Neon). Quem usa o banco é a **API
na VM**, não a Vercel: as variáveis que a integração cria no projeto da Vercel não são lidas pelo front
e só servem para copiar a conexão.

1. Pegue a connection string **direta**, sem pooler: na Vercel é `DATABASE_URL_UNPOOLED` (ou
   `POSTGRES_URL_NON_POOLING`); no console do Neon, *Connect* com **Connection pooling** desligado (host
   sem `-pooler`). O Flyway e o Hikari dependem de estado de sessão que o PgBouncer não preserva.
2. Converta para JDBC: `postgresql://USER:SENHA@HOST/BANCO?sslmode=require` vira
   `SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/BANCO?sslmode=require`, com `DATABASE_USERNAME=USER` e
   `DATABASE_PASSWORD=SENHA` separados (sem usuário e senha na URL). Se a URL tiver
   `channel_binding=require`, pode remover.

## 2. Cloudflare R2 (arquivos)

1. Crie o bucket `tailorly-private`. Não ative acesso público nem domínio público: a API serve os
   arquivos após verificar o dono.
2. *Manage R2 API Tokens* → token com **Object Read & Write** restrito a esse bucket. A API usa as
   **credenciais S3** mostradas ao criar o token (**Access Key ID** e **Secret Access Key**), não o
   "Token value", que é para a API da Cloudflare. O Secret só aparece uma vez.
3. Endpoint: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` **sem** o nome do bucket no final (o
   bucket vai em `TAILORLY_STORAGE_BUCKET`), região `auto`.

## 3. Oracle Cloud (API)

1. Crie uma instância **VM.Standard.A1.Flex** (Ampere) com Ubuntu 22.04 ou 24.04. 2 OCPU / 12 GB já
   sobram; o Always Free permite até 4 OCPU / 24 GB no total.
2. Na *Security List* (ou NSG) da subnet, libere entrada TCP 80 e 443 e UDP 443.
3. Aponte o domínio da API para o IP público (registro A). Sem domínio, use `<ip-com-hifens>.sslip.io`
   (ex.: `152-67-1-2.sslip.io`), que já resolve para o IP e aceita certificado do Let's Encrypt.
4. Na VM:

   ```bash
   scp deploy/oracle/setup-vm.sh ubuntu@IP:~ && ssh ubuntu@IP 'bash setup-vm.sh'
   ```

   O script instala o Docker, abre 80/443 no `iptables` (as imagens Ubuntu da Oracle bloqueiam tudo
   além do SSH, mesmo com a Security List liberada) e cria swap.
5. Crie `~/tailorly/.env` a partir de [`deploy/oracle/.env.example`](../deploy/oracle/.env.example) e
   rode `chmod 600 ~/tailorly/.env`. Gere os segredos com `openssl rand -base64 32` (JWT e
   `TAILORLY_SECRETS_KEY`, um diferente para cada) e `openssl rand -hex 32` (SearXNG).
6. Crie um par de chaves só para o deploy e autorize a pública na VM:

   ```bash
   ssh-keygen -t ed25519 -f tailorly-deploy -N ''
   ssh-copy-id -i tailorly-deploy.pub ubuntu@IP
   ssh-keyscan -t ed25519 IP   # saída vai no segredo ORACLE_SSH_KNOWN_HOSTS
   ```

A IA da produção é sempre um provedor remoto (`TAILORLY_INTERPRETATION_PROVIDER=remote`): não há
Ollama na VM. O Second Brain (vault Obsidian) é recurso de instalação pessoal e fica desligado.

## 4. Vercel (frontend)

1. Projeto importado do GitHub com **Root Directory = `apps/web`** (framework Vite). Cada push publica
   sozinho; `apps/web/vercel.json` só acrescenta o rewrite da SPA (recarregar `/generator` não dá 404)
   e cabeçalhos de cache e segurança.
2. Em *Settings → Environment Variables*, defina `VITE_API_URL=https://<domínio-da-api>` para
   **Production** (e Preview, se usar) e faça um **Redeploy**: a variável entra no build, então mudar o
   valor exige um build novo. Sem ela, o front chama `/v1` no próprio domínio da Vercel e nada funciona.
3. No `.env` da VM, `TAILORLY_CORS_ALLOWED_ORIGINS` precisa incluir o domínio da Vercel (padrões como
   `https://tailorly-*.vercel.app` liberam os previews).

## 5. GitHub (CI/CD)

Crie o ambiente **production** (*Settings → Environments*) com:

| Tipo      | Nome                     | Valor                                                 |
| --------- | ------------------------ | ----------------------------------------------------- |
| Secret    | `ORACLE_HOST`            | IP ou domínio SSH da VM                               |
| Secret    | `ORACLE_USER`            | `ubuntu`                                              |
| Secret    | `ORACLE_SSH_KEY`         | Conteúdo da chave **privada** `tailorly-deploy`       |
| Secret    | `ORACLE_SSH_KNOWN_HOSTS` | Saída do `ssh-keyscan`                                |
| Variable  | `TAILORLY_API_DOMAIN`    | Domínio da API, sem `https://` (para o health check)  |

A imagem vai para `ghcr.io/<dono>/tailorly-api` com o `GITHUB_TOKEN` do workflow; a VM faz login
temporário no GHCR a cada deploy, então o pacote pode continuar privado.

## Operação

- **Logs:** `ssh ubuntu@IP 'cd ~/tailorly && docker compose logs -f api'`.
- **Rollback da API:** troque `TAILORLY_API_IMAGE` no `~/tailorly/.env` por uma tag anterior
  (`ghcr.io/<dono>/tailorly-api:<sha>`) e rode `docker compose up -d`; ou rode o Deploy manualmente
  a partir do commit desejado. Migrations do Flyway não voltam sozinhas: um rollback para antes de uma
  migration exige cuidado com o banco.
- **Rollback do frontend:** *Vercel → Deployments → Promote* em um deploy anterior.
- **Backups:** o Neon Free guarda histórico curto para restauração pontual. O R2 não tem versionamento
  automático; os originais podem ser reenviados, mas currículos gerados excluídos não voltam.
