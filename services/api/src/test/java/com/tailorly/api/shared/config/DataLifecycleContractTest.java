package com.tailorly.api.shared.config;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirma que os novos endpoints de desativação e exclusão aparecem no contrato OpenAPI gerado
 * ({@code /v3/api-docs}), para pegar uma regressão de assinatura sem depender de ferramenta
 * externa de contract testing. Roda só com {@code TAILORLY_TEST_DATABASE_URL} definido, porque o
 * contexto completo precisa do banco real.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=${TAILORLY_TEST_DATABASE_URL}",
    "spring.datasource.username=${TAILORLY_TEST_DATABASE_USER}",
    "spring.datasource.password=${TAILORLY_TEST_DATABASE_PASSWORD}",
    "spring.jpa.show-sql=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "tailorly.extraction.enabled=false",
    "tailorly.anonymous.cleanup-enabled=false",
    "tailorly.account.cleanup-enabled=false",
    "tailorly.security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "tailorly.security.secrets-key=integration-test-secrets-key"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TAILORLY_TEST_DATABASE_URL", matches = ".+")
class DataLifecycleContractTest {
  @Autowired MockMvc mockMvc;

  @Test
  void exposesBaseVersionDeleteAndOwnerDeactivateEndpoints() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths").value(hasKey("/v1/base-versions/{id}")))
        .andExpect(jsonPath("$.paths['/v1/base-versions/{id}']").value(hasKey("delete")))
        .andExpect(jsonPath("$.paths").value(hasKey("/v1/owners/{id}/deactivate-snapshot")))
        .andExpect(jsonPath("$.paths['/v1/owners/{id}/deactivate-snapshot']").value(hasKey("patch")));
  }
}
