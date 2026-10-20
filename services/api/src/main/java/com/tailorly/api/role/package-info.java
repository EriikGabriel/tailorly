/**
 * Papéis de acesso ({@code ROLE_USER}, {@code ROLE_ADMIN}) e sua associação com usuários.
 *
 * <p>Sem rotas HTTP próprias: as roles são criadas e atribuídas na inicialização por
 * {@link com.tailorly.api.shared.security.RoleInitializer}, usando
 * {@link com.tailorly.api.shared.security.AdminEmailPolicy} para decidir quem recebe
 * {@code ROLE_ADMIN}.</p>
 */
package com.tailorly.api.role;
