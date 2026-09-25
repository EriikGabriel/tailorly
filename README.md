# Tailorly

Monorepo para gerar currículos personalizados para cada vaga. O produto recebe uma descrição ou URL de oportunidade, extrai os requisitos, adapta um currículo a partir de um template versionado e disponibiliza os artefatos para download e acompanhamento no Second Brain.

> **Estado atual:** fundação em andamento. O monorepo, a aplicação web, a API Spring Boot, o proxy local, CORS, Actuator e os serviços locais de PostgreSQL e MinIO já existem. O domínio de geração de currículos, autenticação, persistência e integrações externas ainda estão planejados.

## Objetivo e escopo

O Tailorly busca reduzir o trabalho manual de adaptação de currículos, aumentar a aderência às palavras-chave relevantes para sistemas ATS e manter o histórico das candidaturas no vault.

O fluxo alvo é: receber a vaga → normalizar seu conteúdo → extrair requisitos com LLM → preencher um template Typst → gerar PDF/Markdown → persistir metadados e arquivo → registrar a versão no vault.

## Arquitetura alvo

```mermaid
flowchart LR
    User[Pessoa usuária] --> Web[Web\nReact + Vite]
    Web -->|/api| Api[API\nSpring Boot]
    Api --> Parser[Job parser\ntexto ou URL]
    Parser --> Jsoup[Jsoup]
    Api --> Llm[Cliente LLM]
    Llm --> Provider[OpenAI ou Ollama]
    Api --> Template[Template Typst\nversionado]
    Template --> Pdf[Compilação e PDF]
    Api --> Db[(PostgreSQL)]
    Api --> Storage[(MinIO ou disco local)]
    Storage --> Vault[Second Brain\nB03 Resources/CVs]
```

Os blocos à direita da API representam a arquitetura planejada. Hoje a API contém o scaffold Spring Boot, a configuração web e os endpoints de saúde; os componentes de domínio serão introduzidos progressivamente.

## Estrutura do repositório

| Caminho | Papel | Estado |
| --- | --- | --- |
| `apps/web` | Interface React para criação, edição, prévia e download de currículos. | Scaffold disponível |
| `services/api` | API HTTP e orquestração do fluxo de geração. | Scaffold disponível |
| `compose.yaml` | PostgreSQL 17 e MinIO para desenvolvimento local. | Disponível |
| `turbo.json` | Orquestra tarefas do monorepo. | Disponível |
| `pnpm-workspace.yaml` | Define os workspaces `apps/*` e `services/*`. | Disponível |

### Frontend

| Aspecto | Implementação |
| --- | --- |
| Runtime | React 19, TypeScript e Vite 7 |
| Estilos e componentes | Tailwind CSS 4, Base UI e componentes locais |
| Roteamento | TanStack Router, com geração a partir de `apps/web/src/pages` |
| Dados remotos | TanStack Query; o `QueryClient` é compartilhado no contexto do router |
| Estado local | Zustand, inicialmente em `src/stores/workspace-store.ts` |
| Integração local | Proxy de `/api` para `http://localhost:8080` |

`pages/index.tsx` corresponde a `/`; rotas dinâmicas usam `$id.tsx`; e `pages/__root.tsx` define o layout raiz. O arquivo `apps/web/src/routeTree.gen.ts` é gerado pelo plugin do TanStack Router e não deve ser alterado manualmente.

### API

| Aspecto | Implementação atual | Evolução prevista |
| --- | --- | --- |
| Runtime | Java 21 e Spring Boot 3.5 | — |
| HTTP e validação | Spring Web e Validation | Endpoints do domínio e OpenAPI/Swagger |
| Saúde | Spring Actuator com `health` e `info` expostos | Métricas e logs estruturados |
| Acesso web | CORS para o frontend local em `http://localhost:5173` | Política por ambiente e autenticação |
| Parsing de páginas | Dependência Jsoup já incluída | Extração de vagas por URL |
| Dados e arquivos | PostgreSQL e MinIO no Compose | Spring Data JPA, Flyway e adaptadores de storage |

O Actuator pode ser consultado em `http://localhost:8080/actuator/health`. Ainda não há endpoint de geração de currículo, documentação OpenAPI ou autenticação JWT implementados.

## Requisitos

### Requisitos funcionais

| ID | Requisito | Descrição | Prioridade | Estado |
| --- | --- | --- | --- | --- |
| RF01 | Ingestão de vaga | Aceitar descrição da vaga por texto ou URL. | Alta | Planejado |
| RF02 | Extração de requisitos | Identificar responsabilidades, qualificações e palavras-chave relevantes. | Alta | Planejado |
| RF03 | Geração de currículo | Produzir currículo personalizado a partir dos requisitos e do perfil base. | Alta | Planejado |
| RF04 | Exportação | Disponibilizar o currículo em PDF e Markdown. | Média | Planejado |
| RF05 | Metadados e histórico | Salvar autoria, data, status, template e referência do arquivo gerado. | Média | Planejado |
| RF06 | Integração com Second Brain | Copiar/registrar artefatos em `B03 Resources/CVs/` e criar link para uso no Obsidian. | Alta | Planejado |
| RF07 | Autenticação e papéis | Restringir o acesso por JWT e papéis `ADMIN` e `USER`. | Alta | Planejado |
| RF08 | Due diligence de empresas | Consolidar sinais públicos para apoiar a decisão de candidatura, com fatores e fontes rastreáveis. | A definir | Backlog |

### Requisitos não funcionais

| ID | Requisito | Critério ou diretriz | Prioridade |
| --- | --- | --- | --- |
| RNF01 | Desempenho | Gerar um currículo em até 30 segundos, considerando o tempo de provedores externos. | Alta |
| RNF02 | Escalabilidade | Suportar até 100 usuários simultâneos como referência inicial. | Média |
| RNF03 | Segurança | Proteger dados e credenciais; aplicar autenticação, autorização e configuração por ambiente. | Alta |
| RNF04 | Usabilidade | Oferecer uma interface clara para envio da vaga, revisão e download do resultado. | Média |
| RNF05 | Manutenibilidade | Manter responsabilidades separadas, templates versionados, migrações e testes automatizados. | Média |
| RNF06 | Compatibilidade | Suportar as versões atuais dos principais navegadores. | Média |
| RNF07 | Resiliência | Usar Ollama como alternativa quando o provedor remoto de LLM falhar e storage local quando MinIO não estiver disponível. | Média |
| RNF08 | Observabilidade | Expor saúde da aplicação e evoluir para métricas e logs JSON. | Média |

## Modelo de dados planejado

| Entidade | Campos principais | Responsabilidade |
| --- | --- | --- |
| `users` | `id`, `email`, `password_hash`, `enabled` | Credenciais e estado da conta. |
| `roles` | `id`, `name` | Papéis de acesso, como `ADMIN` e `USER`. |
| `user_roles` | `user_id`, `role_id` | Associação muitos-para-muitos entre usuários e papéis. |
| `cv_templates` | `id`, `name`, `content`, `version` | Templates Typst versionados; a fonte de verdade das gerações. |
| `cv_metadata` | `id`, `filename`, `created_at`, `status`, `owner_id`, `template_id` | Histórico e situação de cada currículo gerado. |
| `file_storage` | `id`, `file_path`, `cv_metadata_id` | Referência do artefato no MinIO ou armazenamento local. |

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : possui
    ROLES ||--o{ USER_ROLES : atribui
    USERS ||--o{ CV_METADATA : cria
    CV_TEMPLATES ||--o{ CV_METADATA : origina
    CV_METADATA ||--o{ FILE_STORAGE : referencia
```

## Pré-requisitos

| Ferramenta | Versão |
| --- | --- |
| Node.js | 22 ou superior |
| pnpm | 10.9.0 (gerenciado pelo Corepack) |
| Java | 21 |
| Docker Compose | Opcional, necessário para PostgreSQL e MinIO locais |

## Desenvolvimento

Instale as dependências e inicie os dois workspaces:

```bash
corepack enable
pnpm install
pnpm dev
```

| Serviço | Endereço |
| --- | --- |
| Web | `http://localhost:5173` |
| API | `http://localhost:8080` |
| Health check | `http://localhost:8080/actuator/health` |

Para iniciar apenas uma aplicação:

```bash
pnpm --filter @tailorly/web dev
pnpm --filter @tailorly/api dev
```

Para disponibilizar os serviços de apoio locais:

```bash
docker compose up -d
```

| Serviço | Endereço | Credenciais de desenvolvimento |
| --- | --- | --- |
| PostgreSQL | `localhost:5432/tailorly` | usuário `tailorly`, senha `tailorly` |
| MinIO API | `http://localhost:9000` | usuário `tailorly`, senha `tailorly-dev-password` |
| MinIO Console | `http://localhost:9001` | mesmas credenciais do MinIO |

Essas credenciais existem apenas para o ambiente local. Variáveis de ambiente e segredos de produção não devem ser versionados.

## Comandos de qualidade

| Comando | Finalidade |
| --- | --- |
| `pnpm build` | Gera o build de todos os workspaces. |
| `pnpm test` | Executa a suíte de testes do monorepo. |
| `pnpm lint` | Executa as verificações de lint. |
| `pnpm check` | Executa as checagens configuradas para os workspaces. |

## Próximos passos

- [ ] Versionar o template base Typst, definido como fonte de verdade dos currículos.
- [ ] Implementar `POST /api/cv/generate` para ingestão de texto ou URL.
- [ ] Adicionar extração de requisitos com OpenAI e fallback Ollama.
- [ ] Implementar a geração de PDF/Markdown, persistência com PostgreSQL/Flyway e armazenamento no MinIO.
- [ ] Incluir JWT/RBAC, documentação OpenAPI, testes unitários, de integração e de contrato.
- [ ] Integrar os artefatos gerados ao vault e preparar CI/CD com GitHub Actions.
