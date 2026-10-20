/**
 * Sessões anônimas: permitem a um owner sem conta existir, com token em hash e expiração ou
 * revogação controladas pelo servidor. Hoje cobre apenas o registro, a validação e a revogação
 * administrativa; emissão do token, cotas e migração para conta ainda são planejadas (veja
 * "Uso sem conta e persistência" no {@code README.md} do monorepo).
 */
package com.tailorly.api.session;
