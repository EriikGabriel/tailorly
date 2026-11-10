# API Spring Boot do Tailorly

API HTTP do Tailorly: contas e sessões anônimas, upload e extração do Currículo Base (PDFBox e
OCR seletivo), fatos e revisão auditada, snapshots do Ground Truth, leitura e análise de vagas,
geração de currículos (Typst, ATS e IA), evidências complementares, due diligence e exportação
opcional para o vault. O fluxo de produto está em
[Currículo Base como Ground Truth](../../docs/product/ground-truth.md).

## Estado atual

| Área             | Implementado                                                                                                                                                                                                                                                            | Ainda planejado                                                  |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Contas e acesso  | Cadastro, login com JWT HS256, refresh token opaco rotativo com detecção de reuso, logout e revogação por sessão, roles persistidas (`ROLE_USER`/`ROLE_ADMIN`), alteração de e-mail e desativação                                                                       | Recuperação de senha e exclusão de conta; expiração por ambiente |
| Sessões anônimas | Emissão e autenticação no servidor, TTL, cotas de upload/processamento/geração, limite de taxa, limpeza automática, encerramento pelo visitante, migração do snapshot revisado para conta e revogação administrativa                                                    | —                                                                |
| Currículo Base   | Upload com deduplicação por owner, versão candidata, fila persistente, extração PDFBox/Tesseract por página, interpretação por IA com evidência, revisão auditada, aprovação atômica de snapshots, desativação e exclusão com as gerações derivadas, retenção de contas | Coletor de objetos órfãos; isolamento do parser de PDF           |
| Vagas            | Leitura por link (Gupy, InHire, JSON-LD, HTML genérico) ou texto, análise por IA, versionamento                                                                                                                                                                         | Fixar o IP na conexão (SSRF)                                     |
| Geração          | Templates Typst versionados e editáveis, `POST /v1/cv/generate` do snapshot aprovado, esteira de ATS, enriquecimento com evidências confirmadas, prévia ao vivo, PDF/Markdown no storage, histórico, download e exclusão                                                | Idioma adicional; avaliador de ATS externo                       |
| Due diligence    | Onze fatores a partir de fontes públicas (SearXNG), com trecho verificado; execução automática em segundo plano para contas                                                                                                                                             | Fontes privadas; atualização periódica                           |
| Infraestrutura   | PostgreSQL com Flyway, storage S3 (Garage local, R2 em produção), OpenAPI, Actuator, imagem Docker com Typst e Tesseract publicada no Render ([Deploy](../../docs/deploy.md))                                                                                           | Observabilidade ampliada                                         |

## Documentação

| Documento                                             | Conteúdo                                                           |
| ----------------------------------------------------- | ------------------------------------------------------------------ |
| [Arquitetura](docs/architecture.md)                   | Tecnologias, pacotes por domínio e persistência.                   |
| [Autenticação e autorização](docs/authentication.md)  | JWT, refresh token, roles, chave do JWT e exemplos de requisição.  |
| [Endpoints](docs/endpoints.md)                        | Catálogo de rotas, aliases `/api`, erros e idioma.                 |
| [Documentos e storage](docs/documents-and-storage.md) | Upload, versões, deduplicação e objetos no S3.                     |
| [Extração de PDF](docs/extraction.md)                 | Fila, OCR seletivo, formato dos fatos, limites e testes opcionais. |
| [Acesso anônimo](docs/anonymous-sessions.md)          | Owners, sessões anônimas, limites, limpeza e migração para conta.  |
| [Configuração](docs/configuration.md)                 | Todas as variáveis de ambiente.                                    |

Rodar localmente (Compose, Garage, `.env`, IA local): [Desenvolvimento local](../../docs/development.md).
Contrato OpenAPI e cliente do frontend: [Contrato OpenAPI](../../docs/development.md#contrato-openapi).

## Testes

```bash
./mvnw test           # testes JUnit
./mvnw compile        # só compila
./mvnw clean package  # gera o .jar
```

No workspace: `pnpm --filter @tailorly/api test`, `check` e `build`. Há testes de autenticação,
upload, extração, geração, vagas, sessões anônimas e ciclo de vida dos dados. Os que dependem de
serviços externos são opcionais:

- `ExtractionPersistenceTest` usa PostgreSQL real e só roda com `TAILORLY_TEST_DATABASE_URL`,
  `TAILORLY_TEST_DATABASE_USER` e `TAILORLY_TEST_DATABASE_PASSWORD` apontando para um banco descartável.
- `PdfExtractorTest.realOcrWhenAvailable` e os testes de Typst exercitam o Tesseract e o CLI reais
  quando estão no `PATH`; caso contrário são ignorados (a CI os instala).

Ainda falta uma suíte integrada com Garage e OCR real.
