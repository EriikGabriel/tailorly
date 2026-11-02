# Tailorly

Gera currículos personalizados para cada vaga **sem inventar fatos**. A pessoa envia o Currículo Base
(PDF), revisa e aprova os fatos extraídos — o _Ground Truth_ —, informa uma vaga por link ou texto e
recebe um currículo em PDF e Markdown montado só com o que foi aprovado, com nota de ATS, prévia ao
vivo de um template Typst editável e histórico. Visitantes podem usar sem conta, com sessão temporária
e limites aplicados no servidor.

> Roda no free tier (Vercel + Render + Neon + Cloudflare R2) para uso de poucas
> pessoas. Detalhes e limites em [Deploy](docs/deploy.md).

## Como funciona

1. **Currículo Base:** upload do PDF → extração (texto direto, OCR só quando precisa) → fatos propostos
   por IA, cada um com a página e o trecho de origem.
2. **Revisão:** a pessoa confirma, corrige ou omite cada fato pendente; ao aprovar, o servidor congela um
   snapshot imutável e o torna o Ground Truth ativo.
3. **Vaga:** leitura por link (Gupy, JSON-LD, HTML genérico) ou texto colado, análise de requisitos por IA.
4. **Geração:** o currículo sai do snapshot aprovado e da versão da vaga; a IA ajusta apresentação e
   vocabulário, mas nada de fora dos fatos (ou de evidências complementares confirmadas) entra.
5. **Opcional:** due diligence da empresa e exportação para um vault Obsidian (Second Brain).

## Estrutura

| Caminho            | Papel                                                          |
| ------------------ | -------------------------------------------------------------- |
| `apps/web`         | Interface React + Vite ([README](apps/web/README.md))          |
| `services/api`     | API Spring Boot ([README](services/api/README.md))             |
| `services/searxng` | Configuração do SearXNG local (busca web opcional)             |
| `docs`             | Documentação de produto, arquitetura, desenvolvimento e deploy |
| `compose.yaml`     | PostgreSQL, Garage (S3) e SearXNG para desenvolvimento         |
| `render.yaml`      | Blueprint da API no Render                                     |

## Começando

```bash
corepack enable
pnpm install
bash services/api/init-garage-env.sh   # credenciais locais do Garage (uma vez)
docker compose up -d                   # PostgreSQL, Garage e SearXNG
pnpm dev                               # web em :5173 e API em :8080
```

Pré-requisitos (Node 22+, Java 21, Docker), IA local, variáveis e comandos de qualidade em
[Desenvolvimento local](docs/development.md).

## Documentação

Índice completo em [`docs/README.md`](docs/README.md).

| Quero...                              | Leia                                                             |
| ------------------------------------- | ---------------------------------------------------------------- |
| Entender a decisão central do produto | [Currículo Base como Ground Truth](docs/product/ground-truth.md) |
| Ver o estado e o que falta            | [Requisitos](docs/requirements.md) e [Roadmap](docs/roadmap.md)  |
| Entender as peças e o modelo de dados | [Arquitetura](docs/architecture.md)                              |
| Rodar localmente e contribuir         | [Desenvolvimento local](docs/development.md)                     |
| Publicar                              | [Deploy](docs/deploy.md)                                         |
| Mexer no frontend                     | [`apps/web`](apps/web/README.md)                                 |
| Mexer na API                          | [`services/api`](services/api/README.md)                         |

Quem altera ingestão, extração, revisão, geração, persistência ou autenticação deve ler antes os
invariantes de domínio em [`AGENTS.md`](AGENTS.md).
