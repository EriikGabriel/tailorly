# Documentação do Tailorly

Cada documento trata de um assunto e aponta para os vizinhos. Comece pelo que você precisa.

## Usar e entender o produto

| Documento                                                   | Para quê                                                                           |
| ----------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| [Currículo Base como Ground Truth](product/ground-truth.md) | Decisão central: fatos revisados, versões, pendências, sessões anônimas, exclusão. |
| [Vagas](product/job-postings.md)                            | Leitura por link ou texto, análise por IA e versionamento.                         |
| [Geração de currículos](product/cv-generation.md)           | Esteira de ATS, template Typst editável, artefatos e histórico.                    |
| [Enriquecimento da geração](product/enrichment.md)          | Evidências complementares, reescrita com âncoras, layouts e padrões do perfil.     |
| [Due diligence](product/due-diligence.md)                   | Onze fatores sobre a empresa, com fontes e incerteza.                              |
| [Second Brain / vault](product/second-brain.md)             | Exportação e leitura de notas do vault Obsidian.                                   |
| [Requisitos](requirements.md)                               | RF/RNF e o estado de cada um.                                                      |
| [Roadmap](roadmap.md)                                       | O que está pronto e o que falta.                                                   |

## Rodar e contribuir

| Documento                                   | Para quê                                                                              |
| ------------------------------------------- | ------------------------------------------------------------------------------------- |
| [Desenvolvimento local](development.md)     | Pré-requisitos, Compose, variáveis, IA local, comandos, OpenAPI e CI.                 |
| [Arquitetura](architecture.md)              | Peças do sistema, estrutura do repositório e modelo de dados.                         |
| [Segurança](security.md)                    | Autenticação, rate limit, cabeçalhos, SSRF, CORS e o que ainda falta.                 |
| [`apps/web`](../apps/web/README.md)         | Frontend: estrutura, aliases, design system, estado, telas e testes.                  |
| [`services/api`](../services/api/README.md) | API: arquitetura, autenticação, endpoints, extração, sessões anônimas e configuração. |
| [`AGENTS.md`](../AGENTS.md)                 | Invariantes de domínio para quem altera o código (inclusive agentes).                 |

## Publicar

| Documento           | Para quê                                                                              |
| ------------------- | ------------------------------------------------------------------------------------- |
| [Deploy](deploy.md) | Vercel + Render (free tier) + Neon + Cloudflare R2, variáveis, cold start e rollback. |

## Convenções

- Separe sempre o que **existe** do que é **planejado**; o planejado fica só no [roadmap](roadmap.md)
  (e na lista "Ainda não existe" do documento de cada área).
- Dados de exemplo nunca são apresentados como currículos reais de um visitante.
- Ao mudar comportamento, atualize o documento da área no mesmo commit.
