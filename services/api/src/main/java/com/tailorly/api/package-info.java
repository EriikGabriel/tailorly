/**
 * API HTTP do Tailorly (Spring Boot 3.5 / Java 21).
 *
 * <p>Os pacotes são organizados por domínio de negócio, não por camada técnica. Cada pacote
 * de domínio normalmente contém sua entidade JPA, repositório, serviço, controller REST e um
 * subpacote {@code dto} com os contratos HTTP. Quando um domínio cresce o suficiente para
 * conter mais de um agregado, ele ganha subpacotes próprios (veja
 * {@link com.tailorly.api.base} e {@link com.tailorly.api.extraction}).</p>
 *
 * <ul>
 *   <li>{@link com.tailorly.api.user} — credenciais e estado da conta.</li>
 *   <li>{@link com.tailorly.api.role} — papéis de acesso ({@code ROLE_USER}/{@code ROLE_ADMIN}).</li>
 *   <li>{@link com.tailorly.api.owner} — identidade proprietária de dados (conta ou sessão
 *       anônima) e a regra central de autorização por owner.</li>
 *   <li>{@link com.tailorly.api.session} — sessões anônimas com expiração/revogação.</li>
 *   <li>{@link com.tailorly.api.objects} — metadados de arquivos no storage privado (S3/Garage).</li>
 *   <li>{@link com.tailorly.api.base} — Currículo Base: arquivo enviado e suas versões.</li>
 *   <li>{@link com.tailorly.api.extraction} — pipeline de extração de texto/OCR dos PDFs.</li>
 *   <li>{@link com.tailorly.api.shared} — configuração, segurança, erros e handlers
 *       transversais a todos os domínios.</li>
 * </ul>
 *
 * <p>Para o fluxo de produto completo (Currículo Base como Ground Truth, versionamento,
 * pendências de revisão e sessões anônimas), veja o {@code README.md} na raiz do serviço e do
 * monorepo — esta Javadoc documenta apenas o que já está implementado.</p>
 */
package com.tailorly.api;
