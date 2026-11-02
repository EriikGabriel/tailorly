/**
 * Sessões anônimas: permitem a um owner sem conta existir, com token em hash e expiração ou
 * revogação controladas pelo servidor. Cobre a emissão e a validação do token, as cotas e o limite de
 * taxa, a limpeza de sessões expiradas, o encerramento pelo visitante e a migração do snapshot
 * revisado para uma conta (veja "Uso sem conta e persistência" em
 * {@code docs/product/ground-truth.md} e {@code services/api/docs/anonymous-sessions.md}).
 */
package com.tailorly.api.session;
