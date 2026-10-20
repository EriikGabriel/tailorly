/**
 * Preocupações transversais, compartilhadas por todos os domínios de negócio:
 *
 * <ul>
 *   <li>{@link com.tailorly.api.shared.config} — configuração de segurança, CORS, OpenAPI.</li>
 *   <li>{@link com.tailorly.api.shared.security} — autenticação (JWT, login), autorização e
 *       inicialização de roles.</li>
 *   <li>{@link com.tailorly.api.shared.exception} — exceções de domínio e o formato de erro
 *       HTTP ({@code ApiError}).</li>
 *   <li>{@link com.tailorly.api.shared.handler} — tradução dessas exceções em respostas HTTP
 *       localizadas.</li>
 * </ul>
 */
package com.tailorly.api.shared;
