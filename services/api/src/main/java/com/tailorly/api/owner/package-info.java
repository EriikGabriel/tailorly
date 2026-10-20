/**
 * Identidade proprietária de dados: uma conta autenticada ou uma sessão anônima.
 *
 * <p>{@link com.tailorly.api.owner.Owner} é o ponto de junção entre usuário, Currículo Base,
 * objetos armazenados e sessão anônima — praticamente todo dado pessoal do produto pertence a
 * um owner. {@link com.tailorly.api.owner.OwnerAccessService} concentra a regra de autorização
 * usada pelos demais domínios ({@code base}, {@code extraction}, {@code objects}): apenas o
 * dono da conta ou um {@code ROLE_ADMIN} pode acessar os dados de um owner.</p>
 */
package com.tailorly.api.owner;
