# Arquitetura da API

- Java 21, Spring Boot 3.5, Spring Web, Validation, Security, OAuth2 Resource Server e Spring Data JPA.
- PostgreSQL via JPA e Flyway (`ddl-auto: validate`) para contas, owners, documentos, extrações, fatos/evidências, revisão, snapshots do Ground Truth, vagas, gerações e due diligence. Modelo de dados em [arquitetura](../../../docs/architecture.md#modelo-de-dados).
- Springdoc para OpenAPI/Swagger UI e Actuator para saúde.
- Pacotes em `src/main/java/com/tailorly/api`, organizados por domínio de negócio: `user`, `role`, `owner`, `session`, `objects` (com `objects/storage` para o adaptador S3) e `shared` (`config`, `security`, `exception`, `handler`). `base` e `extraction` são divididos em subpacotes por agregado: `base/document`, `base/version`, `base/fact` (com `group` e `evidence`), `base/review` e `base/snapshot`; `extraction/run`, `extraction/page` e `extraction/processing` (PDFBox/OCR), com `ExtractionQueue`/`ExtractionWorker` na raiz de `extraction` por coordenarem os dois agregados. `extraction/interpreting` separa a interpretação semântica em peças pequenas: `RoutingCurriculumInterpreter` escolhe a estratégia; `AiCurriculumInterpreter` orquestra `CurriculumPromptBuilder` (prompt), `InterpretationResponseParser` (validação da resposta), `CurriculumFactSchema` (categorias e chaves permitidas) e `ExcerptLocator` (localização da evidência na página); `FactValueNormalizer` padroniza os valores. `job` tem a API e a orquestração na raiz, a leitura de links e textos em `job/reader` (download, política de URL e extratores por fonte), a análise por IA em `job/analysis`, além de `job/dto` e `job/persistence`. `cv` divide a geração em `cv/model` (documento montado), `cv/pipeline` (IA de layout e ATS), `cv/render` (Typst e Markdown) e `cv/template`; `CvGenerationService` coordena as três fases e delega a `CvInputResolver` (snapshot e versão da vaga), `CvInputHasher` (reuso de gerações idênticas) e `GeneratedCvFileStore` (gravação dos arquivos). `ai` e `aipreferences` concentram o cliente de IA e as preferências por owner. `diligence` concentra a due diligence de empresas, `search` o cliente de busca web (SearXNG) e `vault` a exportação/leitura do vault Obsidian. `shared/util` guarda utilitários sem domínio, como o `Sha256` usado por todos os hashes de conteúdo. Cada pacote tem um `package-info.java` com a responsabilidade do domínio.
- Mensagens em inglês e português em `src/main/resources`; testes em `src/test`, espelhando a mesma estrutura de pacotes do código principal.

Os endpoints de negócio começam com `/v1`. Actuator e documentação usam os caminhos próprios das ferramentas.

## Persistência

http://127.0.0.1:3900/tailorly-private/_smoke.txt

--aws-sigv4 'aws:amz:garage:s3'\

http://127.0.0.1:3900/tailorly-private/_smoke.txt

Veja a [decisão de produto](../../../docs/product/ground-truth.md) para os invariantes completos.
