/**
 * Autenticação e autorização: cadastro/login
 * ({@link com.tailorly.api.shared.security.AuthenticationController}), emissão de JWT
 * ({@link com.tailorly.api.shared.security.JwtTokenService}), resolução da conta atual
 * ({@link com.tailorly.api.shared.security.CurrentAccountService}), inicialização de roles
 * ({@link com.tailorly.api.shared.security.RoleInitializer} e
 * {@link com.tailorly.api.shared.security.AdminEmailPolicy}) e respostas de erro localizadas
 * para falhas de autenticação/autorização
 * ({@link com.tailorly.api.shared.security.LocalizedSecurityErrorHandler}).
 *
 * <p>A configuração do filtro de segurança e dos beans de JWT (chave, encoder, decoder) vive em
 * {@link com.tailorly.api.shared.config.SecurityConfiguration}, não aqui.</p>
 */
package com.tailorly.api.shared.security;
