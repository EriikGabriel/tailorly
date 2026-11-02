/**
 * Limites de taxa do servidor: por IP antes da autenticação (login, cadastro e teto geral), por
 * dono depois dela (teto geral e rotas caras) e contenção de força bruta de senha. Os do acesso
 * anônimo ficam em {@code com.tailorly.api.session}.
 */
package com.tailorly.api.shared.ratelimit;
